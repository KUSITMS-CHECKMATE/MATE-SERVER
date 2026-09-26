package server.MATE.global.discord.bot.rest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import server.MATE.global.discord.config.DiscordProperties;

import java.time.Duration;
import java.util.Map;

// 게이트웨이 연결 없이 봇 토큰으로 Discord REST를 호출하는 클라이언트. 봇이 꺼진 mate 파드의 메시지 전송용
@Component
public class DiscordBotRestClient {

    private static final String BASE_URL = "https://discord.com/api/v10";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    // 스레드 자동 보관까지의 무활동 시간(분), 7일
    private static final int THREAD_AUTO_ARCHIVE_MINUTES = 10080;
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {
    };

    private final WebClient webClient;
    private final DiscordProperties properties;

    public DiscordBotRestClient(WebClient.Builder builder, DiscordProperties properties) {
        this.webClient = builder.baseUrl(BASE_URL).build();
        this.properties = properties;
    }

    public boolean isConfigured() {
        String token = properties.bot().token();
        return token != null && !token.isBlank();
    }

    public String createMessage(String channelId, Map<String, Object> body) {
        Map<String, Object> response = webClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JSON_MAP)
                .block(TIMEOUT);
        return String.valueOf(response.get("id"));
    }

    public void editMessage(String channelId, String messageId, Map<String, Object> body) {
        webClient.patch()
                .uri("/channels/{channelId}/messages/{messageId}", channelId, messageId)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    public void startThread(String channelId, String messageId, String name) {
        webClient.post()
                .uri("/channels/{channelId}/messages/{messageId}/threads", channelId, messageId)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", name, "auto_archive_duration", THREAD_AUTO_ARCHIVE_MINUTES))
                .retrieve()
                .toBodilessEntity()
                .block(TIMEOUT);
    }

    public Map<String, Object> getMessage(String channelId, String messageId) {
        return webClient.get()
                .uri("/channels/{channelId}/messages/{messageId}", channelId, messageId)
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .retrieve()
                .bodyToMono(JSON_MAP)
                .block(TIMEOUT);
    }

    private String authorization() {
        return "Bot " + properties.bot().token();
    }
}
