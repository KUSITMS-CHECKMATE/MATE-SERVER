package server.MATE.global.discord.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@ExtendWith(MockitoExtension.class)
class TestAlertChannelTest {

    @Mock
    private DiscordWebhookClient webhookClient;

    @Test
    @DisplayName("sendCreated: test-alert-webhook-url로 새 테스트 임베드를 결과 대기 전송한다")
    void sendCreated_sendsEmbedWithTestInfo() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null, null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties, "prod");

        channel.sendCreated(1L, "제목", 200, LocalDateTime.of(2026, 7, 20, 12, 0, 0));

        ArgumentCaptor<DiscordEmbed> captor = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(webhookClient).sendAndWait(eq("test-alert"), eq("https://discord.test/alert"), captor.capture());

        DiscordEmbed embed = captor.getValue();
        assertThat(embed.title()).isEqualTo("🆕 새 테스트 생성");
        assertThat(embed.description())
                .contains("1")
                .contains("제목")
                .contains("200")
                .contains("WAITING")
                .contains("2026-07-20 12:00:00");
    }

    @Test
    @DisplayName("sendCreated: 전송 실패 예외를 그대로 던진다")
    void sendCreated_propagatesFailure() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null, null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties, "prod");
        org.mockito.BDDMockito.willThrow(new IllegalStateException("boom"))
                .given(webhookClient).sendAndWait(any(), any(), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> channel.sendCreated(1L, "제목", 200, LocalDateTime.of(2026, 7, 20, 12, 0, 0)))
                .isInstanceOf(IllegalStateException.class);
    }
}
