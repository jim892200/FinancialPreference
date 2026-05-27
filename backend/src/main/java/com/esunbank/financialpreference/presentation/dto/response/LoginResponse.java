package com.esunbank.financialpreference.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登入成功回應")
public record LoginResponse(

        @Schema(description = "JWT access token")
        String token,

        @Schema(description = "Token 類型", example = "Bearer")
        String tokenType,

        @Schema(description = "有效秒數", example = "3600")
        long expiresIn,

        @Schema(description = "使用者 ID", example = "A1236456789")
        String userId,

        @Schema(description = "使用者名稱", example = "王o明")
        String userName
) {}
