package io.github.hevymcp;

import io.github.hevymcp.config.HevyProperties;
import io.github.hevymcp.config.McpSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({HevyProperties.class, McpSecurityProperties.class})
public class HevyMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(HevyMcpApplication.class, args);
    }
}
