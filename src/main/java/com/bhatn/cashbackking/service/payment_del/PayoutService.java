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
            // GET THE REAL FUND ACCOUNT ID
            String realFundAccountId = getOrCreateFundAccountId(upiId, userId);
            JSONObject payoutRequest = new JSONObject();
            payoutRequest.put("account_number", "7878780080316316"); // Your RazorpayX Account
            payoutRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue()); // Paise
            payoutRequest.put("currency", "INR");
            payoutRequest.put("mode", "UPI");
            payoutRequest.put("purpose", "payout");
            payoutRequest.put("fund_account_id", realFundAccountId); // Linked to the user's UPI

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

    public String getOrCreateFundAccountId(String upiId, String userId) {
        try {
            // STEP 1: Create a Contact (The User)
            JSONObject contactReq = new JSONObject();
            contactReq.put("name", userId); // Usually user's full name
            contactReq.put("email", userId + "@example.com"); // Placeholder or real email
            contactReq.put("type", "customer");
            contactReq.put("reference_id", userId);

            JSONObject contactRes = postToRazorpay("https://api.razorpay.com/v1/contacts", contactReq);
            String contactId = contactRes.getString("id");

            // STEP 2: Create a Fund Account (Link the UPI ID to the Contact)
            JSONObject faReq = new JSONObject();
            faReq.put("contact_id", contactId);
            faReq.put("account_type", "vpa"); // VPA stands for Virtual Payment Address (UPI)
            faReq.put("vpa", new JSONObject().put("address", upiId));

            JSONObject faRes = postToRazorpay("https://api.razorpay.com/v1/fund_accounts", faReq);
            return faRes.getString("id");

        } catch (Exception e) {
            throw new RuntimeException("Failed to register UPI with Razorpay: " + e.getMessage());
        }
    }

    private JSONObject postToRazorpay(String url, JSONObject payload) {
        // 1. Setup Headers with Basic Auth
        String auth = apiKey + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);

        // 2. Execute Request
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return new JSONObject(response.getBody());
        } else {
            throw new RuntimeException("Razorpay API Error: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
}