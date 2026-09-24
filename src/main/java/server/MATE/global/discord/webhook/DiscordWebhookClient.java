package server.MATE.global.discord.webhook;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@Slf4j
@Component
public class DiscordWebhookClient {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public DiscordWebhookClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public void send(String channel, String webhookUrl, DiscordEmbed embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        post(webhookUrl, embed)
                .subscribe(
                        response -> log.info("[DISCORD] 알림 전송 완료. channel={}, title={}", channel, embed.title()),
                        error -> log.warn("[DISCORD] 알림 전송 실패. channel={}, title={}, error={}", channel, embed.title(), error.getMessage())
                );
    }

    // 결과 대기 전송. outbox 재시도처럼 성공 여부 기록이 필요한 호출부용
    public void sendAndWait(String channel, String webhookUrl, DiscordEmbed embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[DISCORD] 웹훅 URL이 설정되지 않아 알림을 보낼 수 없습니다. channel={}, title={}", channel, embed.title());
            throw new IllegalStateException("Discord 웹훅 URL이 설정되지 않았습니다. channel=" + channel);
        }

        try {
            post(webhookUrl, embed)
                    .block(SEND_TIMEOUT);
            log.info("[DISCORD] 알림 전송 완료. channel={}, title={}", channel, embed.title());
        } catch (RuntimeException e) {
            log.warn("[DISCORD] 알림 전송 실패. channel={}, title={}, error={}", channel, embed.title(), e.getMessage());
            throw e;
        }
    }

    private Mono<ResponseEntity<Void>> post(String webhookUrl, DiscordEmbed embed) {
        Map<String, Object> body = Map.of("embeds", List.of(embed.toPayload()));
        return webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity();
    }
}
