package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.dto.ExtractionResult;
import com.bhatn.cashbackking.dto.PayoutStatusResponse;
import com.bhatn.cashbackking.entity.Receipt;
import com.bhatn.cashbackking.entity.ReceiptItem;
import com.bhatn.cashbackking.entity.ReceiptStatus;
import com.bhatn.cashbackking.entity.UserWallet;
import com.bhatn.cashbackking.repository.ReceiptRepository;
import com.bhatn.cashbackking.repository.WalletRepository;
import com.bhatn.cashbackking.service.ReceiptProcessor;
import com.bhatn.cashbackking.service.WalletService;
import com.bhatn.cashbackking.service.ocr.BillAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class ReceiptController {

    private final ReceiptProcessor receiptProcessor;
    private final ReceiptRepository receiptRepository;
    private final WalletRepository walletRepository; // Added for Payout Status
    private final BillAnalyzer billAnalyzer;

    @Autowired
    private WalletService walletService; // Inject your new service

    @Value("${aws.s3.bucket}") // Injected from application.properties
    private String bucketName;

    /**
     * 1. S3 Processing (Production/Android App)
     */
    @PostMapping("/process-s3")
    public ResponseEntity<Map<String, Object>> processS3Receipt(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaimAsString("sub");
        String s3Key = request.get("s3Key");

        log.info("Processing S3 receipt for user: {}", userId);

        Receipt receipt = new Receipt();
        receipt.setUserId(userId);
        receipt.setS3Key(s3Key);
        receipt.setStatus(ReceiptStatus.PROCESSING);
        receipt = receiptRepository.save(receipt);

        // Async processing handles the long-running OCR task
        receiptProcessor.processCashbackAsync(receipt.getId(), bucketName, s3Key);

        return ResponseEntity.ok(Map.of(
                "receiptId", receipt.getId(),
                "status", "PROCESSING_INITIATED"
        ));
    }

    /**
     * 2. Local Upload (Development/Web App)
     */
    @PostMapping("/upload-local")
    public ResponseEntity<Map<String, Object>> uploadLocal(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaimAsString("sub");

        try {
            ExtractionResult result = billAnalyzer.analyze(file.getBytes());

            Receipt receipt = new Receipt();
            receipt.setUserId(userId);
            receipt.setTotalAmount(result.getTotalAmount());
            receipt.setMerchantName(result.getMerchantName());
            receipt.setPurchaseDate(result.getPurchaseDate());
            receipt.setStatus(ReceiptStatus.UPLOADED);
            receipt.setS3Key("local/" + UUID.randomUUID());

            // MAP DTO to ENTITY
            if (result.getLineItems() != null) {
                List<ReceiptItem> entityItems = result.getLineItems().stream().map(dto -> {
                    ReceiptItem item = new ReceiptItem();
                    item.setDescription(dto.getDescription());
                    item.setQuantity(dto.getQuantity());
                    item.setTotalPrice(dto.getPrice()); // Assuming DTO price is the total for that line
                    item.setReceipt(receipt); // CRITICAL for the foreign key link
                    return item;
                }).collect(Collectors.toList());

                receipt.setItems(entityItems);
            }

            receiptRepository.save(receipt);
            receiptProcessor.processCashback(receipt);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "merchant", result.getMerchantName(),
                    "extractedTotal", result.getTotalAmount()
            ));
        } catch (Exception e) {
            log.error("Error processing local upload", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "OCR extraction failed"));
        }
    }

    /**
     * 3. Payout Status (Integrated from BillController)
     * Fetches current balance and ₹30 threshold progress.
     */
    @GetMapping("/payout-status")
    public ResponseEntity<PayoutStatusResponse> getPayoutStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");

        UserWallet wallet = walletRepository.findById(userId)
                .orElseGet(() -> UserWallet.builder()
                        .userId(userId)
                        .currentBalance(BigDecimal.ZERO)
                        .build());

        BigDecimal threshold = new BigDecimal("30.00");
        BigDecimal needed = threshold.subtract(wallet.getCurrentBalance()).max(BigDecimal.ZERO);

        PayoutStatusResponse response = PayoutStatusResponse.builder()
                .currentBalance(wallet.getCurrentBalance())
                .threshold(threshold)
                .statusMessage("₹" + needed + " more needed for payout")
                .recentTransactions(Collections.emptyList()) // Connect to TxnRepo later
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/redeem")
    public ResponseEntity<Map<String, String>> redeem(@AuthenticationPrincipal Jwt jwt) {
        // 1. Get the user ID from the token
        String userId = (jwt != null) ? jwt.getClaimAsString("sub") : "dev-user";

        // 2. Security Check: Verify balance before redeeming
        UserWallet wallet = walletRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getCurrentBalance().compareTo(new BigDecimal("30.00")) < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Insufficient balance. You need at least ₹30.00"
            ));
        }

        // 3. Trigger the redemption logic we wrote in WalletService
        walletService.redeemCashback(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Payout successful! Your balance has been reset.",
                "status", "REDEEMED"
        ));
    }
}