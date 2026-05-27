package com.esunbank.financialpreference.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 簽發 / 解析 JWT。
 *   - HS256
 *   - sub = USER_ID
 *   - typ = "access"
 *   - exp = iat + app.jwt.ttl-minutes
 */
@Service
public class JwtService {

    private static final String CLAIM_TYP = "typ";
    private static final String TYP_ACCESS = "access";

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(JwtProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(props.getTtlMinutes());
    }

    public IssuedToken issue(String userId) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TYP, TYP_ACCESS)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, ttl.toSeconds());
    }

    /**
     * 解析並驗證 token（簽章、過期、typ）。
     * 失敗時拋 {@link JwtException}（包含過期、簽章不符、格式錯誤等）。
     */
    public String parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!TYP_ACCESS.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new JwtException("invalid token type");
        }
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new JwtException("missing subject");
        }
        return sub;
    }

    public record IssuedToken(String token, long expiresInSeconds) {}
}
