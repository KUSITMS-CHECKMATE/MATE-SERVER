package server.MATE.domain.report.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mate.pdf")
public record MatePdfProperties(
        String baseUrl,
        Duration timeout
) {
    public MatePdfProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:3001";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(90);
        }
    }
}
