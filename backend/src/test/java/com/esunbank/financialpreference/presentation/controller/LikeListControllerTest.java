package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import com.esunbank.financialpreference.business.command.UpdateLikeCommand;
import com.esunbank.financialpreference.business.domain.LikeItem;
import com.esunbank.financialpreference.business.domain.Product;
import com.esunbank.financialpreference.business.domain.User;
import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.common.exception.BusinessException;
import com.esunbank.financialpreference.common.exception.ErrorCode;
import com.esunbank.financialpreference.presentation.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 邏輯測試。Security filter 已停用（addFilters = false）；
 * 認證與越權的整合測試在 {@code LikeListControllerSecurityTest}。
 *
 * 透過 SecurityMockMvcRequestPostProcessors.authentication(...) 注入 JWT principal（String），
 * 對應 controller 的 @AuthenticationPrincipal String userId。
 */
@WebMvcTest(LikeListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LikeListControllerTest {

    private static final String USER_A = "A1236456789";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean LikeListService service;

    private static Principal authAs(String userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    // ---------- POST ----------

    @Test
    void create_201_withSnInData() throws Exception {
        when(service.create(any(CreateLikeCommand.class))).thenReturn(42L);

        String body = """
                {
                  "productName": "美元定存",
                  "price": 1000.00,
                  "feeRate": 0.0100,
                  "purchaseQuantity": 5,
                  "account": "1111999666"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").value(42));
    }

    @Test
    void create_blankProductName_returns400Validation() throws Exception {
        String body = """
                {
                  "productName": "",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "a"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.code()));
    }

    @Test
    void create_quantityZero_returns400Validation() throws Exception {
        String body = """
                {
                  "productName": "x",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 0,
                  "account": "a"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.code()));
    }

    @Test
    void create_accountMismatch_returns400() throws Exception {
        when(service.create(any(CreateLikeCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_MISMATCH));

        String body = """
                {
                  "productName": "x",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "wrong-acct"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_MISMATCH.code()));
    }

    // ---------- GET ----------

    @Test
    void list_returnsItemsArrayForAuthenticatedUser() throws Exception {
        LikeItem item = new LikeItem(
                1L,
                new User(USER_A, "王o明", "test@email.com", null),
                new Product(10L, "p", new BigDecimal("100.00"), new BigDecimal("0.0100")),
                5,
                "acct1",
                new BigDecimal("5.00"),
                new BigDecimal("505.00")
        );
        when(service.listByUserId(USER_A)).thenReturn(List.of(item));

        mvc.perform(get("/api/v1/likes").principal(authAs(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data[0].sn").value(1))
                .andExpect(jsonPath("$.data[0].userName").value("王o明"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(505.00));
    }

    @Test
    void list_emptyResult_returnsEmptyArray() throws Exception {
        when(service.listByUserId(USER_A)).thenReturn(List.of());

        mvc.perform(get("/api/v1/likes").principal(authAs(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---------- PUT ----------

    @Test
    void update_returns200() throws Exception {
        String body = """
                {
                  "productName": "改名",
                  "price": 200.00,
                  "feeRate": 0.02,
                  "purchaseQuantity": 3,
                  "account": "acct"
                }
                """;

        mvc.perform(put("/api/v1/likes/{sn}", 1L)
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        verify(service).update(any(UpdateLikeCommand.class));
    }

    @Test
    void update_likeNotFound_returns404() throws Exception {
        doThrow(new BusinessException(ErrorCode.LIKE_NOT_FOUND))
                .when(service).update(any(UpdateLikeCommand.class));

        String body = """
                {
                  "productName": "x",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "a"
                }
                """;

        mvc.perform(put("/api/v1/likes/{sn}", 999L)
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.LIKE_NOT_FOUND.code()));
    }

    @Test
    void update_otherUsersSn_returns403Forbidden() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(service).update(any(UpdateLikeCommand.class));

        String body = """
                {
                  "productName": "x",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "a"
                }
                """;

        mvc.perform(put("/api/v1/likes/{sn}", 1L)
                        .principal(authAs(USER_A))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    // ---------- DELETE ----------

    @Test
    void delete_returns200() throws Exception {
        mvc.perform(delete("/api/v1/likes/{sn}", 1L).principal(authAs(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        verify(service).delete(eq(1L), eq(USER_A));
    }

    @Test
    void delete_likeNotFound_returns404() throws Exception {
        doThrow(new BusinessException(ErrorCode.LIKE_NOT_FOUND))
                .when(service).delete(anyLong(), anyString());

        mvc.perform(delete("/api/v1/likes/{sn}", 999L).principal(authAs(USER_A)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.LIKE_NOT_FOUND.code()));
    }

    @Test
    void delete_otherUsersSn_returns403() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(service).delete(anyLong(), anyString());

        mvc.perform(delete("/api/v1/likes/{sn}", 1L).principal(authAs(USER_A)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    void delete_negativeSn_returns400Validation() throws Exception {
        mvc.perform(delete("/api/v1/likes/{sn}", -1L).principal(authAs(USER_A)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.code()));
    }
}
