package io.github.hevymcp.config;

import io.github.hevymcp.security.AudienceValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuthChallengeEntryPoint authenticationEntryPoint,
            McpSecurityProperties properties) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/mcp", "/mcp/**"))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/**", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/mcp", "/mcp/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/mcp", "/mcp/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/mcp", "/mcp/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .protectedResourceMetadata(metadata -> metadata
                                .protectedResourceMetadataCustomizer(builder -> builder
                                        .resource(properties.resourceUri().toString())
                                        .authorizationServers(servers -> {
                                            servers.clear();
                                            servers.add(properties.issuerUri().toString());
                                        })
                                        .scopes(scopes -> {
                                            scopes.clear();
                                            scopes.addAll(ProtectedResourceMetadataService.SCOPES);
                                        })))
                        .jwt(Customizer.withDefaults()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(McpSecurityProperties properties) {
        return new SupplierJwtDecoder(() -> createJwtDecoder(properties));
    }

    private static JwtDecoder createJwtDecoder(McpSecurityProperties properties) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(
                properties.issuerUri().toString());
        var issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuerUri().toString());
        var audienceValidator = new AudienceValidator(properties.audience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(issuerValidator, audienceValidator));
        return decoder;
    }
}
