package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.dto.PayoutStatusResponse;
import com.bhatn.cashbackking.entity.Transaction;
import com.bhatn.cashbackking.entity.User;
import com.bhatn.cashbackking.entity.UserWallet;
import com.bhatn.cashbackking.repository.TransactionRepository_del;
import com.bhatn.cashbackking.repository.UserRepository_del;
import com.bhatn.cashbackking.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired private UserRepository_del userRepo;
    @Autowired private TransactionRepository_del txnRepo;
    @Autowired private WalletRepository walletRepo;

    @PostMapping("/onboard")
    public ResponseEntity<?> onboardUser(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {

        // 1. Get Cognito ID from the validated JWT
        String cognitoId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        String upiId = request.get( "upiId");

        // 2. Create or Update User profile
        User user = userRepo.findById(cognitoId)
                .orElse(User.builder().cognitoId(cognitoId).build());

        user.setEmail(email);
        user.setUpiId(upiId);
        userRepo.save(user);

        // 3. Initialize an empty Wallet for the user if it doesn't exist
        if (!walletRepo.existsById(cognitoId)) {
            walletRepo.save(new UserWallet(cognitoId, BigDecimal.ZERO, null, null));
        }

        return ResponseEntity.ok(Map.of("message", "User onboarded successfully with UPI: " + upiId));
    }

    @GetMapping("/status")
    public ResponseEntity<PayoutStatusResponse> getWalletStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");

        UserWallet wallet = walletRepo.findByUserId(userId).orElseThrow();
        List<Transaction> txns = txnRepo.findByUserIdOrderByCreatedAtDesc(userId);

        // Map internal transactions to DTOs
        List<PayoutStatusResponse.TransactionDTO> dtos = txns.stream()
                .limit(5)
                .map(t -> PayoutStatusResponse.TransactionDTO.builder()
                        .amount(t.getAmount())
                        .status(t.getStatus().toString())
                        .type(t.getType().toString())
                        .date(t.getCreatedAt().toString())
                        .build())
                .toList();

        return ResponseEntity.ok(PayoutStatusResponse.builder()
                .currentBalance(wallet.getCurrentBalance())
                .threshold(new BigDecimal("30.00"))
                .statusMessage(wallet.getCurrentBalance().compareTo(new BigDecimal("30")) < 0
                        ? "Keep uploading bills to reach ₹30!"
                        : "Payout in progress!")
                .recentTransactions(dtos)
                .build());
    }
}