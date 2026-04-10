package server.MATE.global.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.api")
public record TossApiProperties(
        boolean enabled,
        String baseUrl,
        Timeout timeout
) {
    public record Timeout(
            Duration connect,
            Duration read
    ) {}
}
