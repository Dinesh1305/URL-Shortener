package com.demo.controller;

import com.demo.repository.UrlRepository;
import com.demo.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final UrlRepository urlRepository;
    private final CacheService cacheService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        Map<String, String> response = new LinkedHashMap<>();

        boolean isDbUp = checkDatabase();
        boolean isRedisUp = cacheService.isRedisConnected();

        String status;
        if (isDbUp && isRedisUp) {
            status = "UP";
        } else if (isDbUp) {
            status = "DEGRADED";
        } else {
            status = "DOWN";
        }

        response.put("status", status);
        response.put("database", isDbUp ? "UP" : "DOWN");
        response.put("redis", isRedisUp ? "UP" : "DOWN");

        return ResponseEntity.ok(response);
    }

    private boolean checkDatabase() {
        try {
            urlRepository.count();
            return true;
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
}
