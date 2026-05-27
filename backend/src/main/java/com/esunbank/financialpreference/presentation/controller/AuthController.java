package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.AuthService;
import com.esunbank.financialpreference.presentation.dto.ApiResponse;
import com.esunbank.financialpreference.presentation.dto.request.LoginRequest;
import com.esunbank.financialpreference.presentation.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "認證")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "登入並取得 JWT", description = "成功回 Bearer token；失敗統一回 4011 INVALID_CREDENTIALS")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.userId(), request.password()));
    }
}
