package com.bhatn.cashbackking.service;

import com.bhatn.cashbackking.entity.Transaction;
import com.bhatn.cashbackking.entity.User;
import com.bhatn.cashbackking.entity.UserWallet;
import com.bhatn.cashbackking.repository.TransactionRepository_del;
import com.bhatn.cashbackking.repository.UserRepository_del;
import com.bhatn.cashbackking.repository.WalletRepository;
import com.bhatn.cashbackking.service.payment_del.PaymentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class WalletService_del {

    private static final BigDecimal PAYOUT_THRESHOLD = new BigDecimal("30.00");

    @Autowired private WalletRepository walletRepo;
    @Autowired private UserRepository_del userRepo;
    @Autowired private TransactionRepository_del txnRepo;
    @Autowired private PaymentGateway paymentGateway;

    @Transactional
    public void addCashback(String userId, BigDecimal reward, String s3Key) {
        // 1. Update/Create Wallet
        // 1. Update/Create Wallet
        UserWallet wallet = walletRepo.findByUserId(userId)
                .orElseGet(() -> UserWallet.builder()
                        .userId(userId)
                        .currentBalance(BigDecimal.ZERO)
                        .lastUpdated(LocalDateTime.now())
                        .build());

        wallet.setCurrentBalance(wallet.getCurrentBalance().add(reward));
        walletRepo.save(wallet);

        // 2. Record Cashback Transaction
        txnRepo.save(Transaction.builder()
                .userId(userId)
                .amount(reward)
                .type(Transaction.TransactionType.CASHBACK)
                .status(Transaction.TransactionStatus.COMPLETED)
                .s3Key(s3Key)
                .build());

        // 3. Check for Payout Threshold (₹30)
        if (wallet.getCurrentBalance().compareTo(PAYOUT_THRESHOLD) >= 0) {
            triggerPayout(wallet);
        }
    }

    private void triggerPayout(UserWallet wallet) {
        // 1. Fetch user details to get UPI ID
        User user = userRepo.findById(wallet.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getUpiId() == null) {
            throw new RuntimeException("UPI ID not configured for user: " + user.getCognitoId());
        }

        BigDecimal amountToPay = wallet.getCurrentBalance();

        // 2. Call the Gateway with real data
        String externalPayoutId = paymentGateway.initiateUpiPayout(
                user.getUpiId(),
                amountToPay,
                user.getCognitoId()
        );

        // 3. Record Payout as PENDING
        txnRepo.save(Transaction.builder()
                .userId(user.getCognitoId())
                .amount(amountToPay)
                .type(Transaction.TransactionType.PAYOUT)
                .status(Transaction.TransactionStatus.PENDING)
                .payoutId(externalPayoutId)
                .build());

        // 4. Reset balance
        wallet.setCurrentBalance(BigDecimal.ZERO);
        walletRepo.save(wallet);
    }
    @Transactional
    public void confirmPayoutSuccess(String payoutId) {
        Optional<Transaction> txn = txnRepo.findByPayoutId(payoutId);
        txn.ifPresent(t -> {
            t.setStatus(Transaction.TransactionStatus.COMPLETED);
            txnRepo.save(t);
        });
    }

    @Transactional
    public void handlePayoutFailure(String payoutId) {
        Optional<Transaction> txn = txnRepo.findByPayoutId(payoutId);
        txn.ifPresent(t -> {
            t.setStatus(Transaction.TransactionStatus.FAILED);
            // Refund the wallet since payout failed
            UserWallet wallet = walletRepo.findByUserId(t.getUserId()).orElseThrow();
            wallet.setCurrentBalance(wallet.getCurrentBalance().add(t.getAmount()));
            walletRepo.save(wallet);
            txnRepo.save(t);
        });
    }
}