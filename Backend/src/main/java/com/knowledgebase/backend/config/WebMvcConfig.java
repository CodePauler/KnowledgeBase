package com.knowledgebase.backend.config;

import com.knowledgebase.backend.web.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description: Web层配置，目前只是注册JWT拦截器
 * @date: 2025/12/13 23:41
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")     // 除了/auth（登录注册）外，所有路径都需进行JWT校验
                .excludePathPatterns("/api/auth/**");
    }
}

