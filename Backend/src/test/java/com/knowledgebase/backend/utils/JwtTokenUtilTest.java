package com.knowledgebase.backend.utils;

import com.knowledgebase.backend.security.jwt.JwtTokenUtil;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

public class JwtTokenUtilTest {

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();

    @Test
    void testGenerateAndParseToken() throws InterruptedException {

        String secret = "kfc-crazy-thursday-vme-fifty-dhu-dhu";
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        jwtTokenUtil.setSecretKey(secretKey);
        jwtTokenUtil.setJwtExpiration(2000L);

        // 1. 生成 token
        String token = jwtTokenUtil.generateToken(1L,"testUser");
        System.out.println("生成的 Token: " + token);

        // 2. 从 token 获取用户名
        String username = jwtTokenUtil.getUsernameFromToken(token);
        System.out.println("解析到用户名: " + username);

        // 3. 获取 用户id
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        System.out.println("userId: " + userId);

        // 睡眠5s
        Thread.sleep(5000);

        // 超时验证
        boolean valid = jwtTokenUtil.validateToken(token);
        System.out.println("isValid = " + valid);

        // 超时解析
        try{
            jwtTokenUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
