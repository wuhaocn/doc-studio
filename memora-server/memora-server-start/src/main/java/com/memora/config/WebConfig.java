package com.memora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${memora.web.allowed-origins:http://localhost:8800,http://127.0.0.1:8800}")
    private String allowedOrigins;
    
    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(resolveAllowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION)
            .allowCredentials(true)
            .maxAge(3600);
    }

    private String[] resolveAllowedOrigins() {
        return Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
    }
}
