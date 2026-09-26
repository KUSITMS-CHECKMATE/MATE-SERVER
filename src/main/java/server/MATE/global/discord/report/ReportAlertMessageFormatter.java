package server.MATE.global.discord.report;

import server.MATE.global.common.util.ExceptionTexts;
import server.MATE.global.discord.bot.interaction.ReportBotCustomIds;
import server.MATE.global.discord.webhook.embed.DiscordEmbed;
import server.MATE.global.discord.webhook.embed.EmbedColor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 리포트 집계 알림 메시지 형식. 에러 웹훅은 ErrorAlertChannel, 봇 메시지는 AdminBotMessageFormatter 형식을 따름
public final class ReportAlertMessageFormatter {

    public static final String THREAD_NAME = "처리 기록";
    public static final String API_REQUESTER = "🛠️ 관리자 API로";
    static final int MAX_AI_FAILURE_LINES = 10;
    static final int MAX_REASON_LENGTH = 200;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Pattern CAUSE_LINE = Pattern.compile("⚠️ 원인 \\| `([^`]*)`");
    private static final String CRASH_ANALYSIS =
            "리포트 상태가 IN_PROGRESS에 멈췄을 수 있습니다. DB에서 report_status를 확인해 주세요.";

    public enum MessageState {
        FAILED("리포트 집계 실패", 0xC17E7E, null),
        REAGGREGATING("리포트 재집계 중", 0x82A783, "재집계를 시작했습니다. 완료되면 메이커에게 리포트 완성 알림이 갑니다."),
        COMPLETED("리포트 집계 완료", 0x99AAB5, "리포트 집계가 완료되어 메이커에게 완성 알림을 보냈습니다.");

        private final String label;
        private final int color;
        private final String note;

        MessageState(String label, int color, String note) {
            this.label = label;
            this.color = color;
            this.note = note;
        }
    }

    private ReportAlertMessageFormatter() {
    }

    public static DiscordEmbed aggregationFailedEmbed(ReportAlertTarget target, String cause, String errorMessage,
                                                      Throwable exception, LocalDateTime now, String env) {
        String errorText = exception != null ? ExceptionTexts.describe(exception) : errorMessage;
        String description = String.format(
                "**에러 메시지** : \n```%s```\n" +
                "**시간** : `%s`\n" +
                "**테스트** : `%s`\n" +
                "**원인 분석** : `%s`\n" +
                "**위치** : `%s`\n" +
                "**환경** : `%s`",
                errorText, now.format(TIMESTAMP_FORMAT), target.label(), cause, ExceptionTexts.location(exception), env);
        return new DiscordEmbed("🚨 리포트 집계 실패", description, EmbedColor.ERROR);
    }

    public static DiscordEmbed aggregationCrashedEmbed(ReportAlertTarget target, Throwable exception, LocalDateTime now, String env) {
        String description = String.format(
                "**에러 메시지** : \n```%s```\n" +
                "**시간** : `%s`\n" +
                "**테스트** : `%s`\n" +
                "**원인 분석** : %s\n" +
                "**위치** : `%s`\n" +
                "**환경** : `%s`",
                ExceptionTexts.describe(exception), now.format(TIMESTAMP_FORMAT), target.label(), CRASH_ANALYSIS,
                ExceptionTexts.location(exception), env);
        return new DiscordEmbed("🚨 리포트 집계 처리 오류", description, EmbedColor.ERROR);
    }

    public static DiscordEmbed aiDegradedEmbed(ReportAlertTarget target, int attemptCount,
                                               List<Map.Entry<Long, String>> failures, LocalDateTime now, String env) {
        StringBuilder lines = new StringBuilder();
        int shown = Math.min(failures.size(), MAX_AI_FAILURE_LINES);
        for (int i = 0; i < shown; i++) {
            Map.Entry<Long, String> failure = failures.get(i);
            if (i > 0) {
                lines.append("\n");
            }
            lines.append("Q#").append(failure.getKey()).append(" ").append(truncate(failure.getValue()));
        }
        if (failures.size() > MAX_AI_FAILURE_LINES) {
            lines.append("\n… 외 ").append(failures.size() - MAX_AI_FAILURE_LINES).append("건");
        }
        String description = String.format(
                "**시간** : `%s`\n" +
                "**테스트** : `%s`\n" +
                "**원인 분석** : `%d / %d` 문항 (AI 분석 대상 기준)\n" +
                "```%s```\n" +
                "**환경** : `%s`",
                now.format(TIMESTAMP_FORMAT), target.label(), failures.size(), attemptCount, lines, env);
        return new DiscordEmbed("⚠️ AI 요약 실패", description, EmbedColor.WARN);
    }

    public static Map<String, Object> statusMessageBody(ReportAlertTarget target, String cause, MessageState state) {
        StringBuilder sb = new StringBuilder();
        if (target.description() != null && !target.description().isBlank()) {
            sb.append(target.description()).append("\n\n");
        }
        sb.append(String.format("👥 참여 인원 | `%d명`\n", target.pplCount()));
        sb.append("🕒 마감 | ").append(closedAtText(target)).append("\n");
        sb.append(String.format("⚠️ 원인 | `%s`", cause));
        if (state.note != null) {
            sb.append("\n\n> ").append(state.note);
        }
        DiscordEmbed embed = new DiscordEmbed(state.label + " · " + target.label(), sb.toString(), state.color);
        return Map.of("embeds", List.of(embed.toPayload()), "components", components(target, state));
    }

    public static Map<String, Object> threadEmbedBody(DiscordEmbed embed) {
        return Map.of("embeds", List.of(embed.toPayload()));
    }

    public static Map<String, Object> threadTextBody(String content) {
        return Map.of("content", content);
    }

    public static String memberRequester(String heart, String name) {
        return String.format("%s **%s** 님이", heart, name);
    }

    public static String reaggregateRequestLine(String requester, Instant now) {
        return String.format("%s 재집계 요청 · <t:%d:f>", requester, now.getEpochSecond());
    }

    public static String completedLine(Instant now) {
        return String.format("✅ 리포트 집계 완료 · <t:%d:f>", now.getEpochSecond());
    }

    @SuppressWarnings("unchecked")
    public static String extractCause(Map<String, Object> message) {
        Object embeds = message == null ? null : message.get("embeds");
        if (!(embeds instanceof List<?> list) || list.isEmpty() || !(list.getFirst() instanceof Map<?, ?> first)) {
            return "-";
        }
        Object description = ((Map<String, Object>) first).get("description");
        if (!(description instanceof String text)) {
            return "-";
        }
        Matcher matcher = CAUSE_LINE.matcher(text);
        return matcher.find() ? matcher.group(1) : "-";
    }

    private static String closedAtText(ReportAlertTarget target) {
        if (target.closedAt() == null) {
            return "`-`";
        }
        return String.format("<t:%d:R>", target.closedAt().atZone(KST).toEpochSecond());
    }

    private static List<Map<String, Object>> components(ReportAlertTarget target, MessageState state) {
        if (state != MessageState.FAILED) {
            return List.of();
        }
        Map<String, Object> button = Map.of(
                "type", 2,
                "style", 3,
                "label", "재집계",
                "emoji", Map.of("name", "🔁"),
                "custom_id", ReportBotCustomIds.reaggregate(target.testId()));
        return List.of(Map.of("type", 1, "components", List.of(button)));
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return "(no reason)";
        }
        return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH) + "…";
    }
}
