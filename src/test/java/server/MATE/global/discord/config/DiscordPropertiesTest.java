package server.MATE.global.discord.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordPropertiesTest {

    @Test
    @DisplayName("bot 설정이 없으면 리포트 채널 ID는 빈 문자열")
    void defaultBot_hasBlankReportChannel() {
        DiscordProperties properties = new DiscordProperties("", "", null, null);

        assertThat(properties.bot().reportChannelId()).isEmpty();
    }

    @Test
    @DisplayName("리포트 채널 ID를 설정값 그대로 보관")
    void bot_keepsReportChannel() {
        DiscordProperties properties = new DiscordProperties(
                "", "", new DiscordProperties.Bot(true, "token", "123", "777"), null);

        assertThat(properties.bot().reportChannelId()).isEqualTo("777");
    }
}
