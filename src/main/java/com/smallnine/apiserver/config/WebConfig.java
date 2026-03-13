package com.smallnine.apiserver.config;

import com.smallnine.apiserver.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web配置類
 * 配置攔截器、靜態資源等Web相關設置
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = Paths.get(uploadBaseDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absPath);

        registry.addResourceHandler("/member/member_images/**")
                .addResourceLocations(absPath + "member_images/");
    }
}
