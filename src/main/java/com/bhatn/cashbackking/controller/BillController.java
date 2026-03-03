package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.dto.BillRequest;
import com.bhatn.cashbackking.service.WalletService;
import com.bhatn.cashbackking.service.ocr.BillAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    @Autowired private BillAnalyzer billAnalyzer;
    @Autowired private WalletService walletService;

    @PostMapping("/process")
    public ResponseEntity<?> processUploadedBill(
            @RequestBody BillRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // 1. Get User ID safely from the Cognito JWT token
        String userId = jwt.getClaimAsString("sub");

        try {
            // 2. Extract Total using AWS Textract
            BigDecimal billTotal = billAnalyzer.getBillTotal("your-s3-bucket-name", request.getS3Key());

            // 3. Logic: Give 5% cashback (Adjust as needed)
            BigDecimal cashbackAmount = billTotal.multiply(new BigDecimal("0.05"));

            // 4. Update Wallet & Trigger ₹30 Payout Check
            walletService.addCashback(userId, cashbackAmount, request.getS3Key());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Cashback of ₹" + cashbackAmount + " credited to wallet.",
                    "billTotal", billTotal
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to process bill: " + e.getMessage()
            ));
        }
    }
}