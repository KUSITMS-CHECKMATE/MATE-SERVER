package server.MATE.global.discord.channel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

@Component
public class ErrorAlertChannel {

    private final DiscordWebhookClient webhookClient;
    private final DiscordProperties properties;
    private final String deployEnv;

    public ErrorAlertChannel(
            DiscordWebhookClient webhookClient,
            DiscordProperties properties,
            @Value("${deploy.env:local}") String deployEnv
    ) {
        this.webhookClient = webhookClient;
        this.properties = properties;
        this.deployEnv = deployEnv;
    }

    public void notifyError(ErrorCode errorCode, HttpStatus status, Exception exception, HttpServletRequest request) {
        if ("local".equals(deployEnv)) {
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

        webhookClient.send(properties.errorWebhookUrl(), new DiscordEmbed("🚨 에러 로그", description, EmbedColor.ERROR));
    }
}
