package com.bhatn.cashbackking.dto;

import lombok.Data;

@Data
public class BillRequest {
    private String s3Key; // The path to the image in your S3 bucket
}
