package com.bhatn.cashbackking.entity;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cashback_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CashbackTransaction {
    @Override
    public String toString() {
        return "CashbackTransaction{" +
                "id=" + id +
                ", receiptId=" + receiptId +
                ", userId='" + userId + '\'' +
                ", amountAwarded=" + amountAwarded +
                ", processedAt=" + processedAt +
                ", status=" + status +
                ", remarks='" + remarks + '\'' +
                '}';
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long receiptId; // Links back to the specific receipt that earned this

    @Column(nullable = false)
    private String userId; // The user who earned the reward

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountAwarded; // The specific amount (e.g., ₹2.00)

    @Column(nullable = false)
    private LocalDateTime processedAt; // When the cashback was credited

    @Enumerated(EnumType.STRING)
    private TransactionStatus status; // COMPLETED, REVERSED (for fraud), PAID_OUT

    @Column(length = 500)
    private String remarks; // e.g., "Bonus for first upload" or "Standard 1% cashback"

    public enum TransactionType {
        CASHBACK, PAYOUT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED,
        REDEEMED,   // This amount was part of a ₹30+ payout
        REVERSED
    }
}