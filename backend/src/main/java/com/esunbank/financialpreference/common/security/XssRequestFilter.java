package com.esunbank.financialpreference.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 骨架：Phase 6 將補上 OWASP Java HTML Sanitizer 的清洗邏輯，
// 屆時改為 wrap HttpServletRequest，sanitize body 與 query parameter。
@Component
@Order(1)
public class XssRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);
    }
}
