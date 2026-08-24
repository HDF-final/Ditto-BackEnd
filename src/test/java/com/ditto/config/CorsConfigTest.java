package com.ditto.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class CorsConfigTest {

    private static final String FRONTEND_ORIGIN = "https://ditto-global.com";

    @Test
    void allowsOnlyTheDeployedFrontendOrigin() {
        CorsConfig corsConfig = new CorsConfig(List.of(FRONTEND_ORIGIN));
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/me");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin(FRONTEND_ORIGIN)).isEqualTo(FRONTEND_ORIGIN);
        assertThat(configuration.checkOrigin("http://localhost:3000")).isNull();
        assertThat(configuration.checkOrigin("https://example.trycloudflare.com")).isNull();
        assertThat(configuration.checkOrigin("https://www.ditto-global.com")).isNull();
    }
}
