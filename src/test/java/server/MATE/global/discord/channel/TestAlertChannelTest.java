package server.MATE.global.discord.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.discord.bot.message.AdminBotMessageFormatter;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

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

    private AdminTestListItemResponse item(long id) {
        return new AdminTestListItemResponse(id, "테스트" + id, 300, List.of("DAILY"), TestStatus.WAITING,
                LocalDateTime.of(2026, 9, 24, 1, 0, 0));
    }

    @Test
    @DisplayName("notifyWaitingSummary: 제목·색·항목 줄·초과분·안내 문구를 담아 보낸다")
    void notifyWaitingSummary_sendsListEmbed() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null, null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties, "prod");
        List<AdminTestListItemResponse> items = List.of(item(1), item(2), item(3), item(4), item(5));

        channel.notifyWaitingSummary(items, 7, 4);

        ArgumentCaptor<DiscordEmbed> captor = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(webhookClient).send(eq("test-alert"), eq("https://discord.test/alert"), captor.capture());
        DiscordEmbed embed = captor.getValue();
        assertThat(embed.title()).isEqualTo("검토 대기 목록 · 4시간 이상 7건");
        assertThat(embed.color()).isEqualTo(EmbedColor.WAITING);
        assertThat(embed.description())
                .startsWith("​\n")
                .contains(AdminBotMessageFormatter.listItemLine(item(1)) + "\n\n" + AdminBotMessageFormatter.listItemLine(item(2)))
                .contains(AdminBotMessageFormatter.listItemLine(item(5)))
                .endsWith("\n\n외 2건 · `/tests list` 로 승인·반려");
    }

    @Test
    @DisplayName("notifyWaitingSummary: 초과분이 없으면 '외 N건' 없이 안내 문구만")
    void notifyWaitingSummary_noOverflow() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null, null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties, "prod");

        channel.notifyWaitingSummary(List.of(item(1)), 1, 4);

        ArgumentCaptor<DiscordEmbed> captor = ArgumentCaptor.forClass(DiscordEmbed.class);
        verify(webhookClient).send(eq("test-alert"), eq("https://discord.test/alert"), captor.capture());
        assertThat(captor.getValue().description()).endsWith("\n\n`/tests list` 로 승인·반려");
    }

    @Test
    @DisplayName("notifyWaitingSummary: local 환경이면 보내지 않는다")
    void notifyWaitingSummary_skipsInLocal() {
        DiscordProperties properties = new DiscordProperties("", "https://discord.test/alert", null, null);
        TestAlertChannel channel = new TestAlertChannel(webhookClient, properties, "local");

        channel.notifyWaitingSummary(List.of(item(1)), 1, 4);

        verifyNoInteractions(webhookClient);
    }
}
