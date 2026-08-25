package com.ditto.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import com.ditto.global.exception.ErrorCode;
import com.ditto.global.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectProvider<LocalHeaderAuthenticationFilter> localHeaderAuthenticationFilterProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 인증 없이 접근 가능한 공개 경로 */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/courses/public/**",
            "/api/v1/news/**",
            "/api/v1/ocr/locations/recognize",
            // /swagger-ui.html 은 /swagger-ui/** 와 다른 경로다. 둘 다 열어둬야 리다이렉트가 된다.
            "/swagger-ui.html",
            "/swagger-ui.html/**",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // SPA(별도 오리진) + 세션 쿠키 조합. CSRF 방어는 SameSite 쿠키 정책으로 시작하고,
                // 폼 기반 흐름이 생기면 CookieCsrfTokenRepository 적용을 검토한다.
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // 세션 기반 인증: 필요 시 세션 생성, 로그인 시 세션 고정 공격 방지를 위해 세션 ID 변경
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(1))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, ErrorCode.ACCESS_DENIED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/courses", "/api/v1/community/courses/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/courses/*/comments").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/community/courses/*/comments/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/community/courses/*/comments/*").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/courses/*/likes").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/community/courses/*/likes").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/community/courses/*/bookmarks").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/community/courses/*/bookmarks").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/users/me/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .logout(logout -> logout.disable());

        localHeaderAuthenticationFilterProvider.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class));

        return http.build();
    }

    /**
     * 로그인 처리에 사용할 SecurityContext 저장소.
     * AuthService 에서 인증 성공 후 이 저장소로 SecurityContext 를 HttpSession 에 저장한다.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** AuthService 에서 이메일/비밀번호 인증에 사용 */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
