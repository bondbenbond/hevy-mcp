package io.github.hevymcp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public final class OAuthChallengeEntryPoint implements AuthenticationEntryPoint {

    private static final String SCOPES = "read:workouts read:routines write:routines";
    private final ProtectedResourceMetadataService metadataService;

    public OAuthChallengeEntryPoint(ProtectedResourceMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer resource_metadata=\""
                + metadataService.metadataUri() + "\", scope=\"" + SCOPES + "\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
