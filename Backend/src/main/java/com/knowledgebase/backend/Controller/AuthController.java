package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.entity.User;
import com.knowledgebase.backend.service.UserService;
import com.knowledgebase.backend.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: 授权相关
 * TODO: 登录、登出
 * @date: 2025/12/11 23:33
 * @version: 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public Result<User> register(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam(required = false) String email) {
        try {
            User user = userService.register(username, password, email);
            return Result.success(user,"注册成功");
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestParam String username,
                                            @RequestParam String password){
        try{
            User user = userService.login(username, password);
            String token = jwtTokenUtil.generateToken(user.getId(),user.getUsername());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId",user.getId());
            data.put("username",user.getUsername());

            return Result.success(data,"login success");
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(401,e.getMessage());
        }
    }

    @PostMapping("/refresh") // 刷新token
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(400, "无效的 Authorization 头");
            }

            String oldToken = authHeader.substring(7);

            if (!jwtTokenUtil.validateToken(oldToken)) {
                return Result.error(401, "Token 无效或已过期");
            }

            String username = jwtTokenUtil.getUsernameFromToken(oldToken);
            Long userId = jwtTokenUtil.getUserIdFromToken(oldToken);

            String newToken = jwtTokenUtil.generateToken(userId, username);

            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("userId", userId);
            data.put("username", username);

            return Result.success(data, "Token 刷新成功");

        } catch (Exception e) {
            log.error("刷新 Token 失败", e);
            return Result.error(500, "Token 刷新失败");
        }
    }
}
