package com.demo.controller;

import com.demo.dto.CreateUrlRequest;
import com.demo.dto.UrlResponse;
import com.demo.dto.UrlStatsResponse;
import com.demo.exception.DuplicateAliasException;
import com.demo.exception.GlobalExceptionHandler;
import com.demo.exception.UrlExpiredException;
import com.demo.exception.UrlNotFoundException;
import com.demo.service.RateLimitService;
import com.demo.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlShortenerService urlShortenerService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private UrlShortenerController urlShortenerController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/urls should create short URL and return HTTP 201 CREATED")
    void testCreateShortUrlSuccess() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com/long-page")
                .build();

        UrlResponse response = UrlResponse.builder()
                .shortCode("aB92xK")
                .shortUrl("http://localhost:8080/aB92xK")
                .originalUrl("https://example.com/long-page")
                .createdAt(LocalDateTime.now())
                .build();

        when(rateLimitService.isAllowed(anyString())).thenReturn(true);
        when(urlShortenerService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("aB92xK"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/aB92xK"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/long-page"));
    }

    @Test
    @DisplayName("POST /api/urls should return 409 CONFLICT on duplicate custom alias")
    void testCreateShortUrlDuplicateAlias() throws Exception {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com/dup")
                .customAlias("my-link")
                .build();

        when(rateLimitService.isAllowed(anyString())).thenReturn(true);
        when(urlShortenerService.createShortUrl(any(CreateUrlRequest.class)))
                .thenThrow(new DuplicateAliasException("Custom alias 'my-link' is already in use"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Custom alias 'my-link' is already in use"));
    }

    @Test
    @DisplayName("GET /{shortCode} should return HTTP 307 TEMPORARY REDIRECT with Location header")
    void testRedirectSuccess() throws Exception {
        when(urlShortenerService.getOriginalUrlAndRedirect("aB92xK"))
                .thenReturn("https://example.com/long-page");

        mockMvc.perform(get("/aB92xK"))
                .andExpect(status().isTemporaryRedirect())
                .andExpect(header().string("Location", "https://example.com/long-page"));
    }

    @Test
    @DisplayName("GET /{shortCode} should return 404 NOT FOUND when short code does not exist")
    void testRedirectNotFound() throws Exception {
        when(urlShortenerService.getOriginalUrlAndRedirect("unknown"))
                .thenThrow(new UrlNotFoundException("Short URL with code 'unknown' not found"));

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short URL with code 'unknown' not found"));
    }

    @Test
    @DisplayName("GET /{shortCode} should return 410 GONE when URL has expired")
    void testRedirectExpired() throws Exception {
        when(urlShortenerService.getOriginalUrlAndRedirect("exp123"))
                .thenThrow(new UrlExpiredException("This shortened URL has expired"));

        mockMvc.perform(get("/exp123"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("This shortened URL has expired"));
    }

    @Test
    @DisplayName("GET /api/urls/{shortCode}/stats should return HTTP 200 with stats")
    void testGetStatsSuccess() throws Exception {
        UrlStatsResponse stats = UrlStatsResponse.builder()
                .shortCode("aB92xK")
                .originalUrl("https://example.com")
                .clickCount(125)
                .createdAt(LocalDateTime.now())
                .build();

        when(urlShortenerService.getUrlStats("aB92xK")).thenReturn(stats);

        mockMvc.perform(get("/api/urls/aB92xK/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("aB92xK"))
                .andExpect(jsonPath("$.clickCount").value(125));
    }

    @Test
    @DisplayName("DELETE /api/urls/{shortCode} should return HTTP 204 NO CONTENT")
    void testDeleteUrlSuccess() throws Exception {
        doNothing().when(urlShortenerService).deleteUrl("aB92xK");

        mockMvc.perform(delete("/api/urls/aB92xK"))
                .andExpect(status().isNoContent());

        verify(urlShortenerService).deleteUrl("aB92xK");
    }
}
