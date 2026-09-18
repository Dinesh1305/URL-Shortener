package com.demo.controller;

import com.demo.dto.CreateUrlRequest;
import com.demo.dto.UrlResponse;
import com.demo.dto.UrlStatsResponse;
import com.demo.exception.RateLimitExceededException;
import com.demo.service.RateLimitService;
import com.demo.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimitService rateLimitService;

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        if (!rateLimitService.isAllowed(clientIp)) {
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
        }

        UrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToOriginalUrl(@PathVariable("shortCode") String shortCode) {
        String originalUrl = urlShortenerService.getOriginalUrlAndRedirect(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    @GetMapping("/api/urls/{shortCode}/stats")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable("shortCode") String shortCode) {
        UrlStatsResponse stats = urlShortenerService.getUrlStats(shortCode);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/api/urls/{shortCode}")
    public ResponseEntity<Void> deleteUrl(@PathVariable("shortCode") String shortCode) {
        urlShortenerService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
