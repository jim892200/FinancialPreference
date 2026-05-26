package com.esunbank.financialpreference.presentation.controller;

import com.esunbank.financialpreference.business.command.CreateLikeCommand;
import com.esunbank.financialpreference.business.service.LikeListService;
import com.esunbank.financialpreference.common.config.JacksonXssConfig;
import com.esunbank.financialpreference.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeListController.class)
@Import({GlobalExceptionHandler.class, JacksonXssConfig.class})
class XssIntegrationTest {

    @Autowired MockMvc mvc;
    @MockitoBean LikeListService service;

    @Test
    void postBody_scriptTagInProductName_sanitizedBeforeService() throws Exception {
        when(service.create(any())).thenReturn(99L);

        // <b>bold</b>name -> "boldname"（HTML 標籤剝除、純文字保留）
        String body = """
                {
                  "userId": "A1236456789",
                  "productName": "<b>玉山</b>美元定存",
                  "price": 100.00,
                  "feeRate": 0.0100,
                  "purchaseQuantity": 1,
                  "account": "1111999666"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateLikeCommand> captor = ArgumentCaptor.forClass(CreateLikeCommand.class);
        verify(service).create(captor.capture());
        assertEquals("玉山美元定存", captor.getValue().productName());
    }

    @Test
    void postBody_pureScriptTag_sanitizesToEmpty_then400Validation() throws Exception {
        String body = """
                {
                  "userId": "A1236456789",
                  "productName": "<script>alert(1)</script>",
                  "price": 100.00,
                  "feeRate": 0.0100,
                  "purchaseQuantity": 1,
                  "account": "1111999666"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    @Test
    void postBody_onerrorAttribute_strippedFromAccountField() throws Exception {
        when(service.create(any())).thenReturn(1L);

        String body = """
                {
                  "userId": "A1236456789",
                  "productName": "p",
                  "price": 1.00,
                  "feeRate": 0.01,
                  "purchaseQuantity": 1,
                  "account": "<img src=x onerror=alert(1)>safe"
                }
                """;

        mvc.perform(post("/api/v1/likes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateLikeCommand> captor = ArgumentCaptor.forClass(CreateLikeCommand.class);
        verify(service).create(captor.capture());
        assertEquals("safe", captor.getValue().account());
    }
}
