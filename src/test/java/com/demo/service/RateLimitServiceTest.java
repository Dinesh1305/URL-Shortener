package com.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "maxRequestsPerMinute", 10);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should allow request when limit is not exceeded")
    void testIsAllowedSuccess() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = rateLimitService.isAllowed("127.0.0.1");

        assertTrue(allowed);
        verify(valueOperations).increment("rate_limit:127.0.0.1");
    }

    @Test
    @DisplayName("Should block request when rate limit is exceeded")
    void testIsAllowedExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(11L);

        boolean allowed = rateLimitService.isAllowed("127.0.0.1");

        assertFalse(allowed);
    }

    @Test
    @DisplayName("Should fallback gracefully and allow request when Redis fails")
    void testIsAllowedRedisFailureFallback() {
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        boolean allowed = rateLimitService.isAllowed("127.0.0.1");

        assertTrue(allowed, "Should default to allowed when Redis is down");
    }
}
