package com.bhatn.cashbackking.service.payment_del;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.Base64;

@Service
public class PayoutService implements PaymentGateway {

    @Value("${razorpay.key.id}")
    private String apiKey;

    @Value("${razorpay.key.secret}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String initiateUpiPayout(String upiId, BigDecimal amount, String userId) {
        try {
            String url = "https://api.razorpay.com/v1/payouts";

            JSONObject payoutRequest = new JSONObject();
            payoutRequest.put("account_number", "7878780080316316"); // Your RazorpayX Account
            payoutRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); // Paise
            payoutRequest.put("currency", "INR");
            payoutRequest.put("mode", "UPI");
            payoutRequest.put("purpose", "payout");
            payoutRequest.put("fund_account_id", "fa_00000000000001"); // Linked to the user's UPI

            // Basic Auth Header
            String auth = apiKey + ":" + apiSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(payoutRequest.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                return jsonResponse.getString("id");
            } else {
                throw new RuntimeException("Failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Razorpay Payout Failed: " + e.getMessage());
        }
    }
}