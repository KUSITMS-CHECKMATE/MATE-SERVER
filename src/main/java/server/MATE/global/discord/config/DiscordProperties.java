package server.MATE.global.discord.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        String errorWebhookUrl,
        String testAlertWebhookUrl,
        Bot bot
) {

    public DiscordProperties {
        if (bot == null) {
            bot = new Bot(false, "", "");
        }
    }

    public record Bot(
            boolean enabled,
            String token,
            String adminCommandChannelId
    ) {
    }
}
