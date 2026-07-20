package server.MATE.global.discord.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.domain.test.event.TestCreatedEvent;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;

@ExtendWith(MockitoExtension.class)
class TestAlertChannelTest {

    @Mock
    private DiscordWebhookClient webhookClient;

    @Test
    @DisplayName("test-alert-webhook-url로 생성 알림 임베드를 전송하고 필수 정보를 포함한다")
    void notifyCreated_sendsEmbedWithTestInfo() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties);
        TestCreatedEvent event = new TestCreatedEvent(1L, "제목", 200, LocalDateTime.of(2026, 7, 20, 12, 0, 0));

        channel.notifyCreated(event);

        ArgumentCaptor<DiscordEmbed> captor = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(webhookClient).send(eq("https://discord.test/alert"), captor.capture());

        DiscordEmbed embed = captor.getValue();
        assertThat(embed.description())
                .contains("1")
                .contains("제목")
                .contains("200")
                .contains("WAITING");
    }
}
