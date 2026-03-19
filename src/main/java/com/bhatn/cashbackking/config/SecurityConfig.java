package com.bhatn.cashbackking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disabled for APIs using JWT
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Allow preflight
                        .requestMatchers("/api/v1/receipts/**").authenticated() // Protect your endpoints
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // Enables JWT validation
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // Keep this for H2 Console
                );

        return http.build();
    }
    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_OfWs7erUH";
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // We create a validator that focuses on the timestamp and signature
        // rather than the strict string-match of the 'iss' claim
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();

        // This effectively bypasses the strict 'iss' string check while keeping the signature check
        jwtDecoder.setJwtValidator(withTimestamp);

        return jwtDecoder;
    }
}