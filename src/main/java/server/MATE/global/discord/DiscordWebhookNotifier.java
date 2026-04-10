package server.MATE.global.discord;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import server.MATE.global.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DiscordWebhookNotifier {

    private final WebClient webClient;
    private final String webhookUrl;
    private final String deployEnv;

    public DiscordWebhookNotifier(
            @Value("${discord.webhook-url:}") String webhookUrl,
            @Value("${deploy.env:local}") String deployEnv
    ) {
        this.webhookUrl = webhookUrl;
        this.deployEnv = deployEnv;
        this.webClient = WebClient.builder().build();
    }

    public void notifyError(ErrorCode errorCode, HttpStatus status, Exception exception, HttpServletRequest request) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        String location = Arrays.stream(exception.getStackTrace())
                .filter(e -> e.getClassName().startsWith("server.MATE"))
                .findFirst()
                .map(e -> e.getClassName() + "." + e.getMethodName() + ":" + e.getLineNumber())
                .orElse("N/A");

        String description = String.format(
                "**서비스** : `MATE`\n" +
                "**환경** : `%s`\n" +
                "**시간** : `%s`\n" +
                "**위치** : `%s`\n" +
                "**요청** : `%s %s`\n" +
                "**코드/상태** : `%s / %d`\n" +
                "**에러 메시지** : \n```%s```",
                deployEnv,
                timestamp,
                location,
                request.getMethod(),
                request.getRequestURI(),
                errorCode.getCode(),
                status.value(),
                exception.getMessage() != null ? exception.getMessage() : "(no message)"
        );

        Map<String, Object> embed = Map.of(
                "title", "🚨 에러 로그",
                "description", description,
                "color", 15158332
        );

        Map<String, Object> body = Map.of("embeds", List.of(embed));

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
