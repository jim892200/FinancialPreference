package com.esunbank.financialpreference.common.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 包裝 HttpServletRequest，對 query / form parameter 進行 HTML 清洗。
 * JSON body 的清洗由 JacksonXssConfig 註冊的 String 反序列化器在綁定 DTO 時處理。
 */
@Component
@Order(1)
public class XssRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            chain.doFilter(new SanitizingRequestWrapper(http), response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
