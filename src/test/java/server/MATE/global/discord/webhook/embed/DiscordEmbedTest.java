package server.MATE.global.discord.webhook.embed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscordEmbedTest {

    @Test
    @DisplayName("toPayload는 title, description, color를 그대로 담은 맵을 반환한다")
    void toPayload_returnsMapWithFields() {
        DiscordEmbed embed = new DiscordEmbed("제목", "설명", EmbedColor.INFO);

        assertThat(embed.toPayload())
                .containsEntry("title", "제목")
                .containsEntry("description", "설명")
                .containsEntry("color", EmbedColor.INFO);
    }
}
