package com.esunbank.financialpreference.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 對 query / form parameter 統一套用 HtmlSanitizer。
 * Path variable 與 @RequestBody（JSON）不走這層，由 Spring 型別轉換 / Jackson 自訂反序列化器處理。
 */
public class SanitizingRequestWrapper extends HttpServletRequestWrapper {

    public SanitizingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        return HtmlSanitizer.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = HtmlSanitizer.clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> cleaned = new LinkedHashMap<>(original.size());
        original.forEach((key, values) -> {
            String[] arr = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                arr[i] = HtmlSanitizer.clean(values[i]);
            }
            cleaned.put(key, arr);
        });
        return Collections.unmodifiableMap(cleaned);
    }
}
