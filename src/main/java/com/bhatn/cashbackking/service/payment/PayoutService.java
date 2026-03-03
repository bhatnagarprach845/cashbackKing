package com.bhatn.cashbackking.service.payment;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PayoutService implements PaymentGateway {

    @Autowired private RazorpayClient razorpayClient;

    @Override
    public String initiateUpiPayout(String upiId, BigDecimal amount, String userId) {
        try {
            JSONObject payoutRequest = new JSONObject();
            payoutRequest.put("account_number", "7878780080316316"); // Your RazorpayX Account
            payoutRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); // Paise
            payoutRequest.put("currency", "INR");
            payoutRequest.put("mode", "UPI");
            payoutRequest.put("purpose", "cashback");

            // In a real app, you'd first create a 'Fund Account' in Razorpay for the upiId
            // For simplicity, we assume you have a fund_account_id mapped to the user
            payoutRequest.put("fund_account_id", "fa_00000000000001");

            com.razorpay.Entity payout = razorpayClient.payouts.create(payoutRequest);
            return payout.get("id"); // Returns 'pout_XXXXX'
        } catch (Exception e) {
            throw new RuntimeException("Razorpay Payout Failed: " + e.getMessage());
        }
    }
}