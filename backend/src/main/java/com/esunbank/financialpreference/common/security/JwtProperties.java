package com.esunbank.financialpreference.common.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 設定。secret 必須由環境變數 APP_JWT_SECRET 注入；無 fallback。
 * 長度需 >= 32 bytes 以滿足 HMAC-SHA256 安全強度。
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters")
    private String secret;

    @Min(1)
    private long ttlMinutes = 60;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getTtlMinutes() { return ttlMinutes; }
    public void setTtlMinutes(long ttlMinutes) { this.ttlMinutes = ttlMinutes; }
}
