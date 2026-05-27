package com.esunbank.financialpreference.presentation.dto.request;

import com.esunbank.financialpreference.common.security.RawStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "登入請求")
public record LoginRequest(

        @Schema(description = "使用者 ID", example = "A1236456789", maxLength = 20)
        @NotBlank @Size(max = 20)
        String userId,

        /**
         * 密碼欄位明確跳過全域 XSS 清洗，避免 {@code @} 等字符被改寫成 HTML entity 而導致 BCrypt 比對失敗。
         * 密碼不會被輸出到 HTML，無 XSS 風險。
         */
        @Schema(description = "密碼", example = "Test@1234", maxLength = 100)
        @NotBlank @Size(max = 100)
        @JsonDeserialize(using = RawStringDeserializer.class)
        String password
) {}
