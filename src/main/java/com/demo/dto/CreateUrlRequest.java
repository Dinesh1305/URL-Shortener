package com.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUrlRequest {

    @NotBlank(message = "Original URL cannot be blank")
    private String originalUrl;

    @Size(max = 20, message = "Custom alias cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Custom alias can only contain letters, numbers, hyphens, and underscores")
    private String customAlias;

    @Min(value = 1, message = "Expiration minutes must be at least 1")
    private Integer expiresInMinutes;
}
