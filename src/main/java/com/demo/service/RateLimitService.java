package com.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${urlshortener.rate-limit.requests-per-minute:10}")
    private int maxRequestsPerMinute;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    public boolean isAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }
        String key = RATE_LIMIT_PREFIX + clientIp;
        try {
            Long currentRequests = redisTemplate.opsForValue().increment(key);
            if (currentRequests != null && currentRequests == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(60));
            }
            if (currentRequests != null && currentRequests > maxRequestsPerMinute) {
                log.warn("Rate limit exceeded for IP: {}. Current requests: {}", clientIp, currentRequests);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis error during rate limit check for IP {}. Allowing request. Error: {}", clientIp, e.getMessage());
            return true;
        }
    }
}
