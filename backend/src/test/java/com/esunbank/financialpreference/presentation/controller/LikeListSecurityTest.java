package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端安全整合測試：實際載入 SecurityConfig + JwtAuthenticationFilter，
 * 驗證未帶 token / 帶錯誤 token / 帶合法 token 的行為。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LikeListSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    @MockitoBean LikeListService likeListService;

    @Test
    void noToken_returns401_unauthorized() throws Exception {
        mvc.perform(get("/api/v1/likes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("4010"));
    }

    @Test
    void invalidToken_returns401_unauthorized() throws Exception {
        mvc.perform(get("/api/v1/likes").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("4010"));
    }

    @Test
    void validToken_passesThroughToController() throws Exception {
        when(likeListService.listByUserId(eq("A1236456789"))).thenReturn(List.of());

        String jwt = jwtService.issue("A1236456789").token();

        mvc.perform(get("/api/v1/likes").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));
    }

    @Test
    void loginEndpoint_isPublic_andRejectsBadJson() throws Exception {
        // /api/v1/auth/login 在 allowlist 內，但 body 缺欄位仍回 400 而非 401
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postLike_noToken_returns401() throws Exception {
        String body = """
                {
                  "productName": "x",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "a"
                }
                """;
        mvc.perform(post("/api/v1/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("4010"));
    }
}
