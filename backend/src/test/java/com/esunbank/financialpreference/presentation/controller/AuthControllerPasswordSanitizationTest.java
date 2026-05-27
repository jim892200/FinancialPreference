package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.AuthService;
import com.esunbank.financialpreference.common.config.JacksonXssConfig;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 確保 LoginRequest.password 不被 Jackson XSS 全域 String deserializer 清洗。
 *
 * 沒這個保證的話，OWASP HtmlSanitizer 會把 {@code @} 改成 {@code &#64;}，
 * 使預設帳號 {@code Test@1234} 永遠無法通過 BCrypt 比對。
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonXssConfig.class})
class AuthControllerPasswordSanitizationTest {

    @Autowired MockMvc mvc;
    @MockitoBean AuthService authService;

    @Test
    void password_withAtSign_reachesServiceUnchanged() throws Exception {
        when(authService.login(eq("A1236456789"), eq("Test@1234")))
                .thenReturn(new LoginResponse("jwt", "Bearer", 3600, "A1236456789", "王o明", "1111999666"));

        String body = """
                { "userId": "A1236456789", "password": "Test@1234" }
                """;

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(authService).login("A1236456789", "Test@1234");
    }
}
