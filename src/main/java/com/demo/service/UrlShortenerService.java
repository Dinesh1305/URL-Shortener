package com.demo.service;

import com.demo.dto.CreateUrlRequest;
import com.demo.dto.UrlResponse;
import com.demo.dto.UrlStatsResponse;
import com.demo.entity.Url;
import com.demo.exception.DuplicateAliasException;
import com.demo.exception.InvalidUrlException;
import com.demo.exception.UrlExpiredException;
import com.demo.exception.UrlNotFoundException;
import com.demo.repository.UrlRepository;
import com.demo.util.ShortCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final CacheService cacheService;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${urlshortener.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${urlshortener.short-code.length:6}")
    private int shortCodeLength;

    @Value("${urlshortener.short-code.max-attempts:10}")
    private int maxAttempts;

    @Value("${urlshortener.cache.ttl-minutes:60}")
    private long defaultCacheTtlMinutes;

    public UrlShortenerService(UrlRepository urlRepository, CacheService cacheService, ShortCodeGenerator shortCodeGenerator) {
        this.urlRepository = urlRepository;
        this.cacheService = cacheService;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        validateUrl(request.getOriginalUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            String alias = request.getCustomAlias().trim();
            if (urlRepository.existsByShortCode(alias)) {
                throw new DuplicateAliasException("Custom alias '" + alias + "' is already in use");
            }
            shortCode = alias;
        } else {
            shortCode = generateUniqueShortCode();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = null;
        if (request.getExpiresInMinutes() != null && request.getExpiresInMinutes() > 0) {
            expiresAt = now.plusMinutes(request.getExpiresInMinutes());
        }

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl().trim())
                .shortCode(shortCode)
                .createdAt(now)
                .expiresAt(expiresAt)
                .clickCount(0)
                .build();

        try {
            url = urlRepository.save(url);
            log.info("Successfully created short URL. Code: {}, Original: {}", shortCode, url.getOriginalUrl());
        } catch (DataIntegrityViolationException e) {
            log.error("Conflict while saving short code {}: {}", shortCode, e.getMessage());
            throw new DuplicateAliasException("Short code or custom alias '" + shortCode + "' already exists");
        }

        long ttlSeconds = calculateTtlSeconds(expiresAt);
        cacheService.saveUrl(shortCode, url.getOriginalUrl(), ttlSeconds);

        return buildUrlResponse(url);
    }

    public String getOriginalUrlAndRedirect(String shortCode) {
        Optional<String> cachedUrl = cacheService.getUrl(shortCode);
        if (cachedUrl.isPresent()) {
            urlRepository.incrementClickCount(shortCode);
            log.info("Redirect cache HIT for shortCode: {}", shortCode);
            return cachedUrl.get();
        }

        log.info("Redirect cache MISS for shortCode: {}", shortCode);
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL with code '" + shortCode + "' not found"));

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            cacheService.deleteUrl(shortCode);
            log.warn("Attempted access to expired short URL: {}", shortCode);
            throw new UrlExpiredException("This shortened URL has expired");
        }

        urlRepository.incrementClickCount(shortCode);

        long ttlSeconds = calculateTtlSeconds(url.getExpiresAt());
        if (ttlSeconds > 0) {
            cacheService.saveUrl(shortCode, url.getOriginalUrl(), ttlSeconds);
        }

        return url.getOriginalUrl();
    }

    public UrlStatsResponse getUrlStats(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL with code '" + shortCode + "' not found"));

        return UrlStatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL with code '" + shortCode + "' not found"));

        urlRepository.delete(url);
        cacheService.deleteUrl(shortCode);
        log.info("Successfully deleted short URL with code: {}", shortCode);
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String candidate = shortCodeGenerator.generateShortCode(shortCodeLength);
            if (!urlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new RuntimeException("Failed to generate a unique short code after " + maxAttempts + " attempts");
    }

    private void validateUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new InvalidUrlException("Original URL cannot be blank");
        }
        try {
            URI uri = new URI(urlString.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("URL host cannot be empty");
            }
        } catch (Exception e) {
            throw new InvalidUrlException("Invalid URL format: " + urlString);
        }
    }

    private long calculateTtlSeconds(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return defaultCacheTtlMinutes * 60;
        }
        long remaining = Duration.between(LocalDateTime.now(), expiresAt).toSeconds();
        return Math.max(0, remaining);
    }

    private UrlResponse buildUrlResponse(Url url) {
        String fullShortUrl = baseUrl.endsWith("/") ? baseUrl + url.getShortCode() : baseUrl + "/" + url.getShortCode();
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(fullShortUrl)
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .build();
    }
}
