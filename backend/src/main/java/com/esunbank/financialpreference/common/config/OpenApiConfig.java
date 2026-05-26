package com.esunbank.financialpreference.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financialPreferenceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Financial Preference API")
                        .description("玉山銀行後端工程師實作題 — 金融商品喜好紀錄系統")
                        .version("v1")
                        .contact(new Contact().name("esun-bank-backend-assignment")));
    }
}
