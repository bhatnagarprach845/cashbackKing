package com.bhatn.cashbackking.service;

import com.bhatn.cashbackking.dto.ExtractionResult;
import com.bhatn.cashbackking.entity.*;
import com.bhatn.cashbackking.repository.*;
import com.bhatn.cashbackking.service.ocr.BillAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptProcessor {

    private final ReceiptRepository receiptRepository;
    private final WalletRepository walletRepository;
    private final CashbackTransactionRepository transactionRepository;
    private final BillAnalyzer billAnalyzer;
    private final S3Client s3Client;

    @Async
    @Transactional
    public void processCashbackAsync(Long receiptId, String bucket, String key) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new RuntimeException("Receipt not found"));

        try {
            byte[] imageBytes = downloadFromS3(bucket, key);
            ExtractionResult result = billAnalyzer.analyze(imageBytes);

            // 1. Map Top-Level Data
            receipt.setMerchantName(result.getMerchantName().toUpperCase().trim());
            receipt.setTotalAmount(result.getTotalAmount());
            receipt.setPurchaseDate(result.getPurchaseDate());

            // 2. Map Line Items (The "Gold Mine" for analytics)
            if (result.getLineItems() != null) {
                result.getLineItems().forEach(dto -> {
                    ReceiptItem item = new ReceiptItem();
                    item.setDescription(dto.getDescription());
                    item.setUnitPrice(dto.getPrice());
                    item.setQuantity(dto.getQuantity());

                    // CRITICAL: Link the item back to the receipt
                    item.setReceipt(receipt);
                    receipt.getItems().add(item);
                });
            }

            processCashback(receipt);

        } catch (Exception e) {
            log.error("Async failure for receipt {}: {}", receiptId, e.getMessage());
            receipt.setStatus(ReceiptStatus.FAILED);
            receiptRepository.save(receipt);
        }
    }

    @Transactional
    public void processCashback(Receipt receipt) {
        // 1. Duplicate Check (Excluding the current ID)
        if (isDuplicate(receipt)) {
            log.info("Prachi :: --> Fraud alert");
            receipt.setStatus(ReceiptStatus.REJECTED);
            receiptRepository.save(receipt);
            return;
        }

        // 2. 3% Cashback Calculation
        BigDecimal cashbackAmount = receipt.getTotalAmount()
                .multiply(new BigDecimal("0.03"))
                .setScale(2, RoundingMode.HALF_UP);

        // 3. Update Wallet
        UserWallet wallet = walletRepository.findByUserIdForUpdate(receipt.getUserId())
                .orElseGet(() -> UserWallet.builder().userId(receipt.getUserId()).build());

        wallet.addBalance(cashbackAmount);
        walletRepository.save(wallet);

        // 4. Record Transaction
        CashbackTransaction tx = CashbackTransaction.builder()
                .receiptId(receipt.getId())
                .userId(receipt.getUserId())
                .amountAwarded(cashbackAmount)
                .processedAt(LocalDateTime.now())
                .status(CashbackTransaction.TransactionStatus.COMPLETED)
                .remarks("3% reward: " + receipt.getMerchantName())
                .build();
        transactionRepository.save(tx);

        receipt.setStatus(ReceiptStatus.PROCESSED);
        receiptRepository.save(receipt); // This saves the Receipt AND the Items (Cascade)
    }

    private boolean isDuplicate(Receipt receipt) {
        return receiptRepository.existsByMerchantNameAndTotalAmountAndPurchaseDate(
                receipt.getMerchantName(),
                receipt.getTotalAmount(), receipt.getPurchaseDate()
        );
    }

    private void triggerPayoutNotification(UserWallet wallet) {
        log.info("User {} is eligible for payout! Current Balance: ₹{}",
                wallet.getUserId(), wallet.getCurrentBalance());
        // TODO: Integrate with AWS SNS or a Notification Service for the user
    }

    private byte[] downloadFromS3(String bucket, String key) {
        try {
            log.info("Downloading file from S3: {}/{}", bucket, key);

            // 1. Create the request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            // 2. Fetch the object and convert to byte array
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();

        } catch (S3Exception e) {
            log.error("AWS S3 Error while downloading {}: {}", key, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Could not download receipt from S3", e);
        } catch (Exception e) {
            log.error("Unexpected error downloading from S3: {}", e.getMessage());
            throw new RuntimeException("S3 download failed", e);
        }
    }
}