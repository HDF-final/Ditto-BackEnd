package com.ditto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    private static final String SESSION_COOKIE = "sessionAuth";

    @Bean
    public OpenAPI dittoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DITTO API")
                        .description("K-컬처 관광 플랫폼 DITTO 백엔드 API 명세")
                        .version("v1"))
                // 세션 기반 인증: 로그인 후 발급되는 JSESSIONID 쿠키로 인증한다.
                .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE))
                .components(new Components().addSecuritySchemes(SESSION_COOKIE,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")));
    }
}
