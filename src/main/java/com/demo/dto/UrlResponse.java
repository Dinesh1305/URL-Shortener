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
public class UrlResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
