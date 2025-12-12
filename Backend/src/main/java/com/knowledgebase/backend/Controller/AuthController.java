package com.knowledgebase.backend.controller;

import com.knowledgebase.backend.entity.Result;
import com.knowledgebase.backend.entity.User;
import com.knowledgebase.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            Map<String, Object> data = new HashMap<>();
            data.put("userId",user.getId());
            data.put("username",user.getUsername());

            return Result.success(data,"login success");
        } catch (Exception e) {
            log.error("登录失败", e);
            return Result.error(401,e.getMessage());
        }
    }
}
