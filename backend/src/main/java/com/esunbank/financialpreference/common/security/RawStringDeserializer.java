package com.esunbank.financialpreference.common.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 跳過全域 XSS 清洗的 String deserializer。
 *
 * 用於不會被輸出到 HTML 的敏感欄位（password / token / API key 等）。
 * XSS 清洗會把 {@code @} 之類的字符編成 HTML entity（例如 {@code @} → {@code &#64;}），
 * 對密碼比對而言屬於破壞性處理，故在這類欄位上以 {@code @JsonDeserialize(using=...)} 覆蓋。
 */
public class RawStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return p.getValueAsString();
    }
}
