package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.dto.AdminWalletDTO;
import com.bhatn.cashbackking.dto.PayoutDTO;
import com.bhatn.cashbackking.entity.*;
import com.bhatn.cashbackking.repository.CashbackTransactionRepository;
import com.bhatn.cashbackking.repository.ReceiptRepository;
import com.bhatn.cashbackking.repository.UserRepository;
import com.bhatn.cashbackking.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;

    private final CashbackTransactionRepository transactionRepository;

    @GetMapping("/transactions")
    public List<CashbackTransaction> getAllTransactions() {
        // This will return everything: COMPLETED (earnings) and REDEEMED (payouts)
        return transactionRepository.findAll();
    }

    @GetMapping("/payouts")
    public List<PayoutDTO> getRedeemedHistory() {
        // This fetches only the "REDEEMED" rows for the Admin
        List<CashbackTransaction> redemptions = transactionRepository.findByStatus(CashbackTransaction.TransactionStatus.REDEEMED);
        return redemptions.stream().map(tx -> {
            // Find the user to get their name
            Optional<String> fullname = userRepository.findById(tx.getUserId()).map(User::getName);

            return PayoutDTO.builder()
                    .userId(tx.getUserId())
                    .userName(fullname.orElse( "Unknown User"))
                    .amountAwarded(tx.getAmountAwarded())
                    .processedAt(tx.getProcessedAt())
                    .status(tx.getStatus().toString())
                    .build();
        }).toList();
    }
    @GetMapping("/payouts/report")
    public ResponseEntity<String> exportPayoutsCsv() {
        // Specifically fetch only the redemptions for a payout report
        List<CashbackTransaction> redemptions = transactionRepository.findByStatus(
                CashbackTransaction.TransactionStatus.REDEEMED);

        StringBuilder csv = new StringBuilder();
        csv.append("User ID,Amount Redeemed,Processed Date\n");

        for (CashbackTransaction tx : redemptions) {

            Optional<String> fullname = userRepository.findById(tx.getUserId()).map(User::getName);
            csv.append(fullname.orElse("Unknown User")).append(",")
                    .append(tx.getAmountAwarded()).append(",")
                    .append(tx.getProcessedAt()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=payout_history.csv")
                .header("Content-Type", "text/csv")
                .body(csv.toString());
    }
 /*   @GetMapping("/wallets")
    // For now, we'll keep it simple, but you can add Role-based security here later
    public List<UserWallet> getAllWallets() {
        return walletRepository.findAll();
    }*/

    @GetMapping("/wallets/export")
    public ResponseEntity<String> exportWalletsCsv() {
        List<UserWallet> wallets = walletRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("User ID,Current Balance,Last Updated\n"); // Header

        for (UserWallet wallet : wallets) {
            Optional<String> fullname = userRepository.findById(wallet.getUserId()).map(User::getName);
            csv.append(fullname.orElse(null)).append(",")
                    .append(wallet.getCurrentBalance()).append(",")
                    .append(wallet.getLastUpdated()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=wallets_report.csv")
                .header("Content-Type", "text/csv")
                .body(csv.toString());
    }

    @GetMapping("/wallets")
    public ResponseEntity<List<AdminWalletDTO>> getAdminWallets() {
        return ResponseEntity.ok(walletRepository.findAll().stream().map(wallet -> {
            // Fetch the user details to get the name
            User user = userRepository.findById(wallet.getUserId()).orElse(null);

            return AdminWalletDTO.builder()
                    .userId(wallet.getUserId())
                    .fullName(user != null ? user.getName() : "Unknown User")
                    .email(user != null ? user.getEmail() : "N/A")
                    .upiId(user != null ? user.getUpiId() : "N/A")
                    .currentBalance(wallet.getCurrentBalance())
                    .lastUpdated(wallet.getLastUpdated())
                    .build();
        }).toList());
    }

    // 2. Get all receipts for a specific user
    @GetMapping("/users/{userId}/receipts")
    public ResponseEntity<List<Receipt>> getUserReceipts(@PathVariable String userId) {
        List<Receipt> receipts = receiptRepository.findByUserId(userId);
        return ResponseEntity.ok(receipts);
    }

    /**
     * Fetch specific profile details for the "User Profile" tab
     */
    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<User> getUserProfile(@PathVariable String userId) {
        return userRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}