package server.MATE.global.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "claude.api")
public record ClaudeProperties(String key, String model, int minResponseThreshold, int analysisConcurrency) {
    public ClaudeProperties {
        if (model == null) model = "claude-sonnet-4-6";
        if (minResponseThreshold <= 0) minResponseThreshold = 20;
        if (analysisConcurrency <= 0) analysisConcurrency = 5;
    }
}
