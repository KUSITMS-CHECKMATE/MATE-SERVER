package server.MATE.global.discord.channel;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import server.MATE.domain.test.event.TestCreatedEvent;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

@Component
public class TestAlertChannel {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscordWebhookClient webhookClient;
    private final DiscordProperties properties;

    public TestAlertChannel(DiscordWebhookClient webhookClient, DiscordProperties properties) {
        this.webhookClient = webhookClient;
        this.properties = properties;
    }

    public void notifyCreated(TestCreatedEvent event) {
        String description = String.format(
                "**ID** : `%d`\n" +
                "**제목** : %s\n" +
                "**리워드** : `%d`P\n" +
                "**상태** : `WAITING`\n" +
                "**생성 시각** : `%s`",
                event.testId(),
                event.title(),
                event.reward(),
                event.createdAt().format(TIMESTAMP_FORMAT)
        );

        webhookClient.send(properties.testAlertWebhookUrl(), new DiscordEmbed("🆕 새 테스트 생성", description, EmbedColor.INFO));
    }
}
