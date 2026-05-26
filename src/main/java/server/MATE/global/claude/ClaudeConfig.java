package server.MATE.global.claude;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
public class ClaudeConfig {

    @Bean
    @Qualifier("claudeWebClient")
    public WebClient claudeWebClient(WebClient.Builder builder, ClaudeProperties properties) {
        return builder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", properties.key())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
