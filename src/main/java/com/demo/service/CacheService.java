package com.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "url:";

    public CacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> getUrl(String shortCode) {
        try {
            String key = KEY_PREFIX + shortCode;
            String originalUrl = redisTemplate.opsForValue().get(key);
            if (originalUrl != null) {
                log.debug("Redis cache HIT for shortCode: {}", shortCode);
                return Optional.of(originalUrl);
            }
            log.debug("Redis cache MISS for shortCode: {}", shortCode);
        } catch (Exception e) {
            log.warn("Redis unavailable during getUrl for shortCode {}. Fallback to MySQL. Error: {}", shortCode, e.getMessage());
        }
        return Optional.empty();
    }

    public void saveUrl(String shortCode, String originalUrl, long ttlSeconds) {
        try {
            String key = KEY_PREFIX + shortCode;
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, originalUrl, Duration.ofSeconds(ttlSeconds));
            } else {
                redisTemplate.opsForValue().set(key, originalUrl);
            }
            log.debug("Saved shortCode {} to Redis with TTL {}s", shortCode, ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis unavailable during saveUrl for shortCode {}. Error: {}", shortCode, e.getMessage());
        }
    }

    public void deleteUrl(String shortCode) {
        try {
            String key = KEY_PREFIX + shortCode;
            redisTemplate.delete(key);
            log.debug("Evicted shortCode {} from Redis cache", shortCode);
        } catch (Exception e) {
            log.warn("Redis unavailable during deleteUrl for shortCode {}. Error: {}", shortCode, e.getMessage());
        }
    }

    public boolean isRedisConnected() {
        try {
            String response = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(response);
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
