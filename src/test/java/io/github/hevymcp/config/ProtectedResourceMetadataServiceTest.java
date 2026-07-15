package io.github.hevymcp.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ProtectedResourceMetadataServiceTest {

    @Test
    void buildsEndpointSpecificMetadataUri() {
        var properties = new McpSecurityProperties(
                URI.create("https://issuer.example/"), "audience", URI.create("https://mcp.example/mcp"));
        var service = new ProtectedResourceMetadataService(properties);

        assertThat(service.metadataUri()).hasToString(
                "https://mcp.example/.well-known/oauth-protected-resource/mcp");
        assertThat(service.metadata().resource()).isEqualTo("https://mcp.example/mcp");
        assertThat(service.metadata().authorizationServers()).containsExactly("https://issuer.example/");
        assertThat(service.metadata().scopesSupported()).containsExactly(
                "read:workouts", "read:routines", "write:routines", "read:exercise_templates",
                "read:exercise_history", "read:routine_folders");
    }

    @Test
    void buildsRootMetadataUriForRootResource() {
        var properties = new McpSecurityProperties(
                URI.create("https://issuer.example/"), "audience", URI.create("https://mcp.example"));

        assertThat(new ProtectedResourceMetadataService(properties).metadataUri()).hasToString(
                "https://mcp.example/.well-known/oauth-protected-resource");
    }
}
