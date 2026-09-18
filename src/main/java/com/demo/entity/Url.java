package com.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "urls",
    indexes = {
        @Index(name = "idx_short_code", columnList = "short_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_short_code", columnNames = "short_code")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
