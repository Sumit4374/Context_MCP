package com.context_mcp.context_mcp.config;

import com.context_mcp.context_mcp.infrastructure.security.RegexSecretDetector;
import com.context_mcp.context_mcp.infrastructure.security.SecretDetector;
import com.context_mcp.context_mcp.infrastructure.security.SecretRedactor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContextPlatformProperties.class)
public class ContextPlatformAutoConfiguration {

    @Bean
    public SecretDetector secretDetector() {
        return new RegexSecretDetector();
    }

    @Bean
    public SecretRedactor secretRedactor(SecretDetector secretDetector) {
        return new SecretRedactor(secretDetector);
    }
}
