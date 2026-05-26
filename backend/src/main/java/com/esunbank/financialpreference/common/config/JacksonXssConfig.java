package com.esunbank.financialpreference.common.config;

import com.esunbank.financialpreference.common.security.HtmlSanitizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * 註冊一個全域 Jackson Module，所有從 JSON body 反序列化出來的 String 都會被 HtmlSanitizer 清洗。
 * 凡是進入 Controller 的 @RequestBody DTO 字串欄位都會自動套用，無須每個 DTO 個別處理。
 */
@Configuration
public class JacksonXssConfig {

    @Bean
    public Module xssStringDeserializerModule() {
        SimpleModule module = new SimpleModule("xss-string-sanitizer");
        module.addDeserializer(String.class, new JsonDeserializer<>() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return HtmlSanitizer.clean(p.getValueAsString());
            }
        });
        return module;
    }
}
