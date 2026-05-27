package com.esunbank.financialpreference.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 提供 CorsConfigurationSource bean，讓 Spring Security 的 http.cors() 與 MVC handler 共用。
 * Security filter chain 在 JwtAuthenticationFilter 之前處理 preflight (OPTIONS)，需共用同一份 CORS 設定。
 */
@Configuration
public class WebMvcConfig {

    private final List<String> allowedOrigins;

    public WebMvcConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.allowedOrigins = List.of(origins.split("\\s*,\\s*"));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
