package com.bhatn.cashbackking.service;

import com.bhatn.cashbackking.entity.CashbackTransaction;
import com.bhatn.cashbackking.entity.UserWallet;
import com.bhatn.cashbackking.repository.CashbackTransactionRepository;
import com.bhatn.cashbackking.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final CashbackTransactionRepository transactionRepository;

    @Transactional
    public void redeemCashback(String userId) {
        // 1. Get all 'COMPLETED' (unredeemed) transactions
        // Note: Ensure your Repository has findByUserIdAndStatus
        List<CashbackTransaction> unredeemed = transactionRepository.findByUserIdAndStatus(
                userId, CashbackTransaction.TransactionStatus.COMPLETED);

        // 2. Mark them as REDEEMED
        unredeemed.forEach(tx -> tx.setStatus(CashbackTransaction.TransactionStatus.REDEEMED));
        transactionRepository.saveAll(unredeemed);

        // 3. Reset the Wallet Balance to ₹0 (Locking is important here!)
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));

        wallet.setCurrentBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
    }
}