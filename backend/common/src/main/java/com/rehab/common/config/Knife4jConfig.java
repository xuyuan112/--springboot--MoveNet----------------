package com.rehab.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc OpenAPI 3 文档配置。
 * 访问地址：{@code http://localhost:8080/api/v1/doc.html}（仅非生产环境开放）。
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer JWT";

    @Bean
    public OpenAPI rehabOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MoveNet 康复训练平台 API")
                        .description("基于 MoveNet 姿态评估的康复训练辅助平台接口文档（/api/v1）")
                        .version("v1.0")
                        .contact(new Contact().name("rehab-platform").email("dev@example.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
