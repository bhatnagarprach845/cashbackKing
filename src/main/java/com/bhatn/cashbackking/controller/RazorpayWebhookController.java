package com.bhatn.cashbackking.controller;

import com.bhatn.cashbackking.service.WalletService_del;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
public class RazorpayWebhookController {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Autowired private WalletService_del walletServiceDel;

    @PostMapping
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // 1. Verify that the request actually came from Razorpay
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Signature");
            }

            JSONObject jsonEvent = new JSONObject(payload);
            String event = jsonEvent.getString("event");

            // 2. Handle Payout Success
            if ("payout.processed".equals(event)) {
                JSONObject payoutEntity = jsonEvent.getJSONObject("payload")
                        .getJSONObject("payout")
                        .getJSONObject("entity");

                String payoutId = payoutEntity.getString("id");
                walletServiceDel.confirmPayoutSuccess(payoutId);
            }

            // 3. Handle Payout Failure (e.g., wrong UPI ID)
            else if ("payout.reversed".equals(event)) {
                String payoutId = jsonEvent.getJSONObject("payload")
                        .getJSONObject("payout")
                        .getJSONObject("entity")
                        .getString("id");
                walletServiceDel.handlePayoutFailure(payoutId);
            }

            return ResponseEntity.ok("Webhook Received");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}