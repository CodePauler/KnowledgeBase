package com.knowledgebase.backend.web.interceptor;

import com.knowledgebase.backend.security.jwt.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * @description: JWT拦截器
 * @date: 2025/12/13 23:38
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * @return boolean 放行与否
     * @description : 在请求处理之前进行JWT验证
     * @date 2025/12/13 23:40
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行登录、注册等接口
        if (request.getRequestURI().startsWith("/api/auth")) {
            return true;
        }

        // 2. 取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 3. 提取 token
        String token = authHeader.substring(7);

        // 4. 校验 token
        if (!jwtTokenUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        // 5. 解析 userId
        Long userId = jwtTokenUtil.getUserIdFromToken(token);

        // 6. 注入请求上下文
        request.setAttribute("userId", userId);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
