package io.github.hevymcp.config;

import io.github.hevymcp.HevyMcpApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        classes = HevyMcpApplication.class,
        properties = {
                "hevy.api-key=fake-test-key",
                "hevy.base-url=http://127.0.0.1:1",
                "mcp.security.issuer-uri=https://issuer.example/",
                "mcp.security.audience=test-audience",
                "mcp.security.resource-uri=https://mcp.example/mcp",
                "debug=false"
        })
class RouteSecurityTest {

    @Autowired
    WebApplicationContext context;

    @MockitoBean
    JwtDecoder jwtDecoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        org.mockito.Mockito.when(jwtDecoder.decode("invalid-token"))
                .thenThrow(new BadJwtException("Invalid test token"));
    }

    @Test
    void mcpRoutesRequireBearerAuthentication() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString(
                        "resource_metadata=\"https://mcp.example/.well-known/oauth-protected-resource/mcp\"")));
        mvc.perform(get("/mcp/session/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtIsRejected() throws Exception {
        mvc.perform(post("/mcp").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metadataIsPublicAtRootAndEndpointSpecificPaths() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("https://mcp.example/mcp"))
                .andExpect(jsonPath("$.authorization_servers[0]").value("https://issuer.example/"));
        mvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
                .andExpect(status().isOk());
    }

    @Test
    void healthIsPublicAndUnrelatedRoutesAreDenied() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/not-an-endpoint")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMcpRequestPassesRouteAuthentication() throws Exception {
        mvc.perform(post("/mcp")
                        .with(jwt().jwt(token -> token.audience(java.util.List.of("test-audience")))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "SCOPE_read:workouts")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> {
                    if (result.getResponse().getStatus() == 401) {
                        throw new AssertionError("Authenticated MCP request was rejected by route security");
                    }
                });
    }
}
