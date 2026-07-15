package io.github.hevymcp.config;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
public final class ProtectedResourceMetadataService {

    public static final List<String> SCOPES = List.of(
            "read:workouts", "read:routines", "write:routines", "read:exercise_templates",
            "read:exercise_history", "read:routine_folders");

    private final McpSecurityProperties properties;

    public ProtectedResourceMetadataService(McpSecurityProperties properties) {
        this.properties = properties;
    }

    public ProtectedResourceMetadata metadata() {
        return new ProtectedResourceMetadata(
                properties.resourceUri().toString(),
                List.of(properties.issuerUri().toString()),
                SCOPES);
    }

    public URI metadataUri() {
        URI resource = properties.resourceUri();
        String path = resource.getPath();
        String suffix = (path == null || path.isBlank() || "/".equals(path)) ? "" : path;
        return URI.create(resource.getScheme() + "://" + resource.getAuthority()
                + "/.well-known/oauth-protected-resource" + suffix);
    }

    public record ProtectedResourceMetadata(
            String resource,
            List<String> authorizationServers,
            List<String> scopesSupported) {
    }
}
