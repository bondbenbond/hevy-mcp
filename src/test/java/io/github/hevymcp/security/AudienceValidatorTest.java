package io.github.hevymcp.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("expected-audience");

    @Test
    void acceptsCorrectAudience() {
        assertThat(validator.validate(jwt(List.of("expected-audience"))).hasErrors()).isFalse();
    }

    @Test
    void rejectsMissingAudience() {
        assertThat(validator.validate(jwt(List.of())).hasErrors()).isTrue();
    }

    @Test
    void rejectsIncorrectAudience() {
        assertThat(validator.validate(jwt(List.of("another-audience"))).hasErrors()).isTrue();
    }

    @Test
    void acceptsExpectedAudienceAmongMultipleValues() {
        assertThat(validator.validate(jwt(List.of("another-audience", "expected-audience"))).hasErrors()).isFalse();
    }

    private static Jwt jwt(List<String> audience) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("not-a-real-token")
                .header("alg", "none")
                .subject("test-user")
                .audience(audience)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
