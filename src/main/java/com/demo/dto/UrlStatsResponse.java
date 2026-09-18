package com.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlStatsResponse {

    private String shortCode;
    private String originalUrl;
    private long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
