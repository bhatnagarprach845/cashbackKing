package com.bhatn.cashbackking.service;

import com.bhatn.cashbackking.entity.User;
import com.bhatn.cashbackking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    @Transactional
    public User syncUser(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        return userRepo.findById(sub)
                .map(existingUser -> {
                    // Update existing user if info changed in Cognito
                    existingUser.setEmail(email);
                    existingUser.setName(name);
                    return userRepo.save(existingUser);
                })
                .orElseGet(() -> {
                    // Create new user record if it's their first time
                    User newUser = User.builder()
                            .cognitoId(sub)
                            .email(email)
                            .name(name)
                            .build();
                    return userRepo.save(newUser);
                });
    }
}
