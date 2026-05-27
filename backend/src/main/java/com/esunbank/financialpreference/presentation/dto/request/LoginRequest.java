package com.esunbank.financialpreference.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "登入請求")
public record LoginRequest(

        @Schema(description = "使用者 ID", example = "A1236456789", maxLength = 20)
        @NotBlank @Size(max = 20)
        String userId,

        @Schema(description = "密碼", example = "Test@1234", maxLength = 100)
        @NotBlank @Size(max = 100)
        String password
) {}
