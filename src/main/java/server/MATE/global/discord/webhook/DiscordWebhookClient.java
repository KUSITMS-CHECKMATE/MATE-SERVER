package server.MATE.global.discord.webhook;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@Slf4j
@Component
public class DiscordWebhookClient {

    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(3);
    private static final int RETRY_MAX_ATTEMPTS = 2;
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofMillis(500);
    // 최대 3회 시도(ATTEMPT_TIMEOUT * 3) + 백오프 지연 여유분
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(12);

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
                .toBodilessEntity()
                .timeout(ATTEMPT_TIMEOUT)
                .retryWhen(Retry.backoff(RETRY_MAX_ATTEMPTS, RETRY_MIN_BACKOFF)
                        .filter(DiscordWebhookClient::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    // 연결/응답 타임아웃 등 네트워크 계층 실패만 재시도. 잘못된 웹훅 URL(4xx) 등은 재시도해도 성공할 수 없으므로 제외
    private static boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientRequestException
                || throwable instanceof TimeoutException;
    }
}
