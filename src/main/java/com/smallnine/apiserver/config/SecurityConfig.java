package com.smallnine.apiserver.config;

import com.smallnine.apiserver.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrls;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * OAuth2 filter chain — only matches /oauth2/** and /login/oauth2/**
     * Needs session for authorization code flow state parameter.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http,
                                                  AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                                  AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) throws Exception {
        http
            .securityMatcher("/oauth2/**", "/login/oauth2/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorize"))
                .redirectionEndpoint(redir -> redir.baseUri("/oauth2/callback/*"))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * API filter chain — stateless JWT for all other paths.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 認證相關（只開登入/註冊/刷新）
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh",
                        "/api/auth/verify-email", "/api/auth/resend-verification").permitAll()

                // Swagger
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/docs/**", "/v3/api-docs/**").permitAll()

                // 收藏類公開統計端點（必須在 authenticated 之前）
                .requestMatchers(HttpMethod.GET, "/api/articles/favorites/top").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/favorites/top").permitAll()
                // 其餘收藏操作一律需要認證
                .requestMatchers("/api/articles/favorites/**").authenticated()
                .requestMatchers("/api/products/favorites/**").authenticated()

                // 唯讀瀏覽（只開 GET）
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupon/coupons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/brands/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/sitter/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vip-levels/**").permitAll()

                // 靜態資源（圖片）
                .requestMatchers(HttpMethod.GET, "/member/member_images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                // 健康檢查
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/log-test/**").permitAll()

                // 其餘一律要 JWT
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"未登入或令牌已過期\",\"data\":null,\"success\":false}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(frontendUrls.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
