package com.bhatn.cashbackking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
// Ensure Spring looks for your repositories in the right place
@EnableJpaRepositories("com.bhatn.cashbackking.repository")
public class CashbackKingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbackKingApplication.class, args);
    }
}