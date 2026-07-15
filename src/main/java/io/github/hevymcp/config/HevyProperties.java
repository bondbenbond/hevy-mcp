package io.github.hevymcp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties("hevy")
public record HevyProperties(
        @NotBlank String apiKey,
        @NotNull URI baseUrl) {
}
