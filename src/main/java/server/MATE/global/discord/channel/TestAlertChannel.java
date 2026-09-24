package server.MATE.global.discord.channel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.global.discord.bot.message.AdminBotMessageFormatter;
import server.MATE.global.discord.config.DiscordProperties;
import server.MATE.global.discord.webhook.DiscordWebhookClient;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

@Component
public class TestAlertChannel {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ZWSP = "\u200B";

    private final DiscordWebhookClient webhookClient;
    private final DiscordProperties properties;
    private final String deployEnv;

    public TestAlertChannel(
            DiscordWebhookClient webhookClient,
            DiscordProperties properties,
            @Value("${deploy.env:local}") String deployEnv
    ) {
        this.webhookClient = webhookClient;
        this.properties = properties;
        this.deployEnv = deployEnv;
    }

    // outbox 발송 경로 전용. 결과 대기 후 실패 시 예외를 던져 재시도로 위임
    public void sendCreated(long testId, String title, int reward, LocalDateTime createdAt) {
        String description = String.format(
                "**ID** : `%d`\n" +
                "**제목** : %s\n" +
                "**리워드** : `%d`P\n" +
                "**상태** : `WAITING`\n" +
                "**생성 시각** : `%s`",
                testId,
                title,
                reward,
                createdAt.format(TIMESTAMP_FORMAT)
        );

        webhookClient.sendAndWait("test-alert", properties.testAlertWebhookUrl(), new DiscordEmbed("🆕 새 테스트 생성", description, EmbedColor.INFO));
    }

    // 관리자 봇 /tests list와 같은 줄 모양으로 장기 대기 테스트 요약. 다음 주기에 다시 나가므로 fire-and-forget 발송
    public void notifyWaitingSummary(List<AdminTestListItemResponse> items, long totalCount, int thresholdHours) {
        if ("local".equals(deployEnv) || items.isEmpty()) {
            return;
        }

        String title = String.format("검토 대기 목록 · %d시간 이상 %d건", thresholdHours, totalCount);
        String lines = items.stream()
                .map(AdminBotMessageFormatter::listItemLine)
                .collect(Collectors.joining("\n\n"));
        long overflow = totalCount - items.size();
        String footer = (overflow > 0 ? "외 " + overflow + "건 · " : "") + "`/tests list` 로 승인·반려";

        String description = ZWSP + "\n" + lines + "\n\n" + footer;
        webhookClient.send("test-alert", properties.testAlertWebhookUrl(), new DiscordEmbed(title, description, EmbedColor.WAITING));
    }
}
