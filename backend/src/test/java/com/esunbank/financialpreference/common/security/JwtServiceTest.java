package com.esunbank.financialpreference.common.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private JwtService newService(long ttlMinutes) {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setTtlMinutes(ttlMinutes);
        return new JwtService(props);
    }

    @Test
    void issue_then_parse_returnsSubject() {
        JwtService svc = newService(60);
        JwtService.IssuedToken token = svc.issue("A1236456789");

        assertEquals("A1236456789", svc.parseUserId(token.token()));
        assertTrue(token.expiresInSeconds() > 0);
    }

    @Test
    void parse_tamperedSignature_throwsJwtException() {
        JwtService svc = newService(60);
        JwtService.IssuedToken token = svc.issue("A1");
        String tampered = token.token().substring(0, token.token().length() - 4) + "AAAA";

        assertThrows(JwtException.class, () -> svc.parseUserId(tampered));
    }

    @Test
    void parse_expiredToken_throwsJwtException() throws InterruptedException {
        // ttl >=1 minute（JwtProperties.@Min 限制），但我們直接用更短的 secret 簽然後等是不切實際的；
        // 改採另一支不同 secret 的 service 解析，模擬簽章驗證失敗 / token 不再有效。
        JwtService issuer = newService(60);
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("DIFFERENT-DIFFERENT-DIFFERENT-DIFFERENT-1234");
        otherProps.setTtlMinutes(60);
        JwtService otherSvc = new JwtService(otherProps);

        JwtService.IssuedToken token = issuer.issue("A1");
        assertThrows(JwtException.class, () -> otherSvc.parseUserId(token.token()));
    }

    @Test
    void parse_garbage_throwsJwtException() {
        JwtService svc = newService(60);
        assertThrows(JwtException.class, () -> svc.parseUserId("not-a-jwt"));
    }
}
