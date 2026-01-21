package com.smallnine.apiserver.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smallnine API")
                        .description("寵物服務網站 API 文檔")
                        .version("1.0.0")
                        .termsOfService("https://smallnine.com/terms")
                        .contact(new Contact()
                                .name("SmallNine Development Team")
                                .email("contact@smallnine.com")
                                .url("https://smallnine.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地開發服務器"), //port在8080
                        new Server()
                                .url("https://api.smallnine.com")
                                .description("生產環境服務器")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("專案網址")
                        .url("https://github.com/c879873067877881111/Bark-Bijou-backend/tree/main"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")
                                        .description("請輸入 JWT Token\n\n格式: Bearer {token}\n\n" +
                                                "1. 首先調用 /api/auth/login 獲取 JWT Token\n" +
                                                "2. 然後在此處輸入: Bearer {獲取的token}")
                        )
                        // 添加常用的回應模式
                        .addSchemas("ApiResponse", new io.swagger.v3.oas.models.media.Schema<>()
                                .type("object")
                                .addProperty("code", new io.swagger.v3.oas.models.media.Schema<>().type("integer").description("回應程式碼"))
                                .addProperty("message", new io.swagger.v3.oas.models.media.Schema<>().type("string").description("回應訊息"))
                                .addProperty("success", new io.swagger.v3.oas.models.media.Schema<>().type("boolean").description("是否成功"))
                                .addProperty("data", new io.swagger.v3.oas.models.media.Schema<>().description("回應數據"))
                        )
                );
    }
}