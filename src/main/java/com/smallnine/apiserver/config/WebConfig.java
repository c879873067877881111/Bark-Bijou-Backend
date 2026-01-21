package com.smallnine.apiserver.config;

import com.smallnine.apiserver.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置類
 * 配置攔截器等Web相關設置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final LoggingInterceptor loggingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")  // 攔截所有API請求
                .excludePathPatterns(
                        "/api/health",       // 排除健康檢查
                        "/swagger-ui/**",    // 排除Swagger UI
                        "/v3/api-docs/**",   // 排除API文檔
                        "/swagger-ui.html"   // 排除Swagger首頁
                );
    }
}