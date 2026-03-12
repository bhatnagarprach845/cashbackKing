package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.entity.UserWallet;
import com.bhatn.cashbackking.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WalletRepository walletRepository;

    @GetMapping("/wallets")
    // For now, we'll keep it simple, but you can add Role-based security here later
    public List<UserWallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @GetMapping("/wallets/export")
    public ResponseEntity<String> exportWalletsCsv() {
        List<UserWallet> wallets = walletRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("User ID,Current Balance,Last Updated\n"); // Header

        for (UserWallet wallet : wallets) {
            csv.append(wallet.getUserId()).append(",")
                    .append(wallet.getCurrentBalance()).append(",")
                    .append(wallet.getLastUpdated()).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=wallets_report.csv")
                .header("Content-Type", "text/csv")
                .body(csv.toString());
    }
}