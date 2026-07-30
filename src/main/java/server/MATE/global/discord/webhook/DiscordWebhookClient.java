package server.MATE.global.discord.webhook;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@Slf4j
@Component
public class DiscordWebhookClient {

    private final WebClient webClient;

    public DiscordWebhookClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public void send(String webhookUrl, DiscordEmbed embed) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        Map<String, Object> body = Map.of("embeds", List.of(embed.toPayload()));

        webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.info("Discord 알림 전송 완료"),
                        error -> log.warn("Discord 알림 전송 실패: {}", error.getMessage())
                );
    }
}
