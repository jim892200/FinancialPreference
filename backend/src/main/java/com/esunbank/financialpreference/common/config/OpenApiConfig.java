package com.esunbank.financialpreference.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI financialPreferenceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Financial Preference API")
                        .description("金融商品喜好紀錄系統（含 JWT 認證）")
                        .version("v1")
                        .contact(new Contact().name("financial-preference")))
                .components(new Components().addSecuritySchemes(JWT_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("由 POST /api/v1/auth/login 取得")));
    }
}
