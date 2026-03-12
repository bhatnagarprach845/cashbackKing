package com.bhatn.cashbackking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // Cognito ID

    private Long receiptId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // CASHBACK or PAYOUT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // PENDING, COMPLETED, FAILED

    private String s3Key;       // Reference to the receipt image (only for CASHBACK)
    private String payoutId;    // External ID from Razorpay (only for PAYOUT)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", receiptId=" + receiptId +
                ", amount=" + amount +
                ", type=" + type +
                ", status=" + status +
                ", s3Key='" + s3Key + '\'' +
                ", payoutId='" + payoutId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        CASHBACK, PAYOUT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED
    }
}