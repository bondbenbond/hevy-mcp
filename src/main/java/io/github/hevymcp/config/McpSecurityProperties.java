package io.github.hevymcp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("mcp.security")
public record McpSecurityProperties(
        @NotNull URI issuerUri,
        @NotBlank String audience,
        @NotNull URI resourceUri) {
}
