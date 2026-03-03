package com.bhatn.cashbackking.repository;

import com.bhatn.cashbackking.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<UserWallet, String> {

    /**
     * PESSIMISTIC_WRITE lock ensures that if two processes try to update
     * the same wallet, one must wait for the other. This prevents
     * "double-spend" or incorrect balance updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserWallet> findByUserId(String userId);

    // Custom query to find all wallets that are eligible for a payout
    // List<UserWallet> findByBalanceGreaterThanEqual(BigDecimal threshold);
}