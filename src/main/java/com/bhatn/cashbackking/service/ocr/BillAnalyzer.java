package com.bhatn.cashbackking.service.ocr;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.S3Object;

import java.math.BigDecimal;

@Component
public class BillAnalyzer {
    @Autowired
    private TextractClient textractClient;

    public BigDecimal getBillTotal(String bucket, String key) {
        AnalyzeExpenseRequest request = AnalyzeExpenseRequest.builder()
                .document(Document.builder()
                        .s3Object(S3Object.builder().bucket(bucket).name(key).build())
                        .build())
                .build();

        AnalyzeExpenseResponse response = textractClient.analyzeExpense(request);

        // Logic to extract the specific 'TOTAL' field from bill summary
        return response.expenseDocuments().get(0).summaryFields().stream()
                .filter(f -> f.type().text().equals("TOTAL"))
                .map(f -> new BigDecimal(f.valueDetection().text().replaceAll("[^\\d.]", "")))
                .findFirst().orElse(BigDecimal.ZERO);
    }
}