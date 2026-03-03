package com.bhatn.cashbackking.repository;

import com.bhatn.cashbackking.entity.Transaction;
import com.bhatn.cashbackking.entity.Transaction.TransactionStatus;
import com.bhatn.cashbackking.entity.Transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Fetch the full history for a specific user, sorted by newest first
    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find a specific payout by the external Razorpay ID (used in Webhooks)
    Optional<Transaction> findByPayoutId(String payoutId);

    // Filter transactions by status (e.g., for a dashboard showing pending items)
    List<Transaction> findByUserIdAndStatus(String userId, TransactionStatus status);

    // Find all cashback entries related to a specific bill image
    Optional<Transaction> findByS3Key(String s3Key);

    // Count how many payouts a user has received
    long countByUserIdAndType(String userId, TransactionType type);
}