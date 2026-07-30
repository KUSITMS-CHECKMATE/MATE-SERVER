package server.MATE.global.discord.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        String errorWebhookUrl,
        String testAlertWebhookUrl,
        Bot bot,
        Members members
) {

    public DiscordProperties {
        if (bot == null) {
            bot = new Bot(false, "", "");
        }
        if (members == null) {
            members = new Members(List.of(), List.of(), List.of(), List.of());
        }
    }

    public record Bot(
            boolean enabled,
            String token,
            String adminCommandChannelId
    ) {
    }

    public record Members(
            List<Long> backend,
            List<Long> frontend,
            List<Long> planning,
            List<Long> design
    ) {
        public Members {
            backend = backend == null ? List.of() : backend;
            frontend = frontend == null ? List.of() : frontend;
            planning = planning == null ? List.of() : planning;
            design = design == null ? List.of() : design;
        }
    }
}
