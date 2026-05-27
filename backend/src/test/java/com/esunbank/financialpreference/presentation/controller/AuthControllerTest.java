package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.AuthService;
import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.presentation.advice.GlobalExceptionHandler;
import com.esunbank.financialpreference.presentation.dto.response.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;

    @Test
    void login_success_returnsToken() throws Exception {
        when(authService.login(eq("A1236456789"), eq("Test@1234")))
                .thenReturn(new LoginResponse("jwt.token.value", "Bearer", 3600, "A1236456789", "王o明"));

        String body = """
                { "userId": "A1236456789", "password": "Test@1234" }
                """;

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.token").value("jwt.token.value"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userId").value("A1236456789"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(eq("A1"), eq("bad")))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        String body = """
                { "userId": "A1", "password": "bad" }
                """;

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.code()));
    }

    @Test
    void login_blankUserId_returns400() throws Exception {
        String body = """
                { "userId": "", "password": "x" }
                """;

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.code()));
    }
}
