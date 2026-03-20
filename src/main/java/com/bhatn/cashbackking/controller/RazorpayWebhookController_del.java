package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.service.WalletService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
public class RazorpayWebhookController_del {

    /*@Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private WalletService walletService; // Using your active WalletService

    @PostMapping
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // 1. Verify Signature (Security)
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Signature");

            JSONObject jsonEvent = new JSONObject(payload);
            String event = jsonEvent.getString("event");

            // Navigate the JSON tree to get the Payout ID
            JSONObject payout = jsonEvent.getJSONObject("payload")
                    .getJSONObject("payout")
                    .getJSONObject("entity");
            String payoutId = payout.getString("id");

            // 2. Handle Logic based on event type
            if ("payout.processed".equals(event)) {
                // Marks the transaction as COMPLETED in your DB
                walletService.confirmPayoutSuccess(payoutId);
            }
            else if ("payout.reversed".equals(event) || "payout.failed".equals(event)) {
                // Marks transaction as FAILED and REFUNDS the specific amount back to the wallet
                walletService.handlePayoutFailure(payoutId);
            }

            return ResponseEntity.ok("Webhook Received");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }*/
}