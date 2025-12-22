package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PracticeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtConfigService {

    private final PracticeService practiceService;

    @Value("${jwt.expiration:1800000}")
    private long defaultJwtExpiration;

    public long getJwtExpirationMillis() {
        try {
            var practicesResponse = practiceService.getAllPractices();
            if (practicesResponse.getData() != null && !practicesResponse.getData().isEmpty()) {
                PracticeDto practice = practicesResponse.getData().get(0);
                Integer tokenExpiryMinutes = practice.getTokenExpiryMinutes();
                
                if (tokenExpiryMinutes != null && tokenExpiryMinutes >= 5 && tokenExpiryMinutes <= 30) {
                    long expirationMillis = tokenExpiryMinutes * 60 * 1000L;
                    log.debug("Using practice token expiry: {} minutes ({}ms)", tokenExpiryMinutes, expirationMillis);
                    return expirationMillis;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get practice token expiry, using default: {}", e.getMessage());
        }
        
        log.debug("Using default JWT expiration: {}ms", defaultJwtExpiration);
        return defaultJwtExpiration;
    }
}