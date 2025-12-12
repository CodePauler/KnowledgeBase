package com.knowledgebase.backend.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * @description: JWT令牌签发与解析
 * @date: 2025/12/12 19:29
 */
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Setter
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Setter
    private SecretKey secretKey;

    @PostConstruct // 一旦BEAN初始化完成之后，将会调用这个方法。避免多次构造secretkey
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /** 签发token */
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey)
                .compact();
    }

    /**
     * @param token: 传入的令牌
     * @return Claims 返回payload
     * @description 解析令牌，返回payload
     * @date 2025/12/12 20:07
     */
    private Claims parseToken(String token) { // 异常直接往上抛，不仔细区分了
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 获取 userId */
    public Long getUserIdFromToken(String token) {
        // 优先使用 claims 里的 userId，如果没有则 fallback 取 sub
        Claims claims = parseToken(token);
        Long userId = claims.get("userId", Long.class);
        if (userId != null) return userId;
        return Long.valueOf(parseToken(token).getSubject());
    }

    /** 获取用户名 */
    public String getUsernameFromToken (String token) {
        return parseToken(token).get("username", String.class);
    }

    /** 验证token */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
