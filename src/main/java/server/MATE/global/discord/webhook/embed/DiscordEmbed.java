package server.MATE.global.discord.webhook.embed;

import java.util.Map;

public record DiscordEmbed(
        String title,
        String description,
        int color
) {

    public Map<String, Object> toPayload() {
        return Map.of(
                "title", title,
                "description", description,
                "color", color
        );
    }
}
