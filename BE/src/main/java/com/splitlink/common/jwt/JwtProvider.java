package com.splitlink.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성, 검증 및 정보 추출을 전담하는 컴포넌트
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds = 1000L * 60 * 60 * 24 * 7; // 만료 기간: 7일

    public JwtProvider(@Value("${jwt.secret-key}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 방 접속 사용자를 위한 액세스 토큰 발급
     */
    public String createToken(Long roomId, Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .claim("roomId", roomId)
                .claim("memberId", memberId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 서명 유효성 및 만료 여부 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        return getClaims(token).get("memberId", Long.class);
    }

    public Long getRoomId(String token) {
        return getClaims(token).get("roomId", Long.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
