package server.MATE.global.discord.bot.message;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import server.MATE.domain.test.dto.response.AdminTestDetailResponse;
import server.MATE.domain.test.dto.response.AdminTestListItemResponse;
import server.MATE.domain.test.dto.response.AdminTestListResponse;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.discord.bot.interaction.AdminBotCustomIds;

public final class AdminBotMessageFormatter {

    // 콘텐츠 헤더
    public static final String HEADER_LIST = "**📋 테스트 목록 조회**";
    public static final String HEADER_DETAIL = "**🔍 테스트 상세 조회**";
    public static final String HEADER_APPROVE = "**✅ 테스트 승인**";
    public static final String HEADER_REJECT = "**❌ 테스트 반려**";
    public static final String HEADER_CREATED = "**🆕 새로운 테스트 등록**";

    private static final Color COLOR_WAITING = new Color(0xD1A85E);
    private static final Color COLOR_IN_PROGRESS = new Color(0x82A783);
    private static final Color COLOR_REJECTED = new Color(0xC17E7E);
    private static final Color COLOR_COMPLETED = new Color(0x99AAB5);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ZWSP = "​";

    private AdminBotMessageFormatter() {
    }

    // 스타일
    public static String statusLabel(TestStatus status) {
        return switch (status) {
            case WAITING -> "검토 대기";
            case IN_PROGRESS -> "진행 중";
            case REJECTED -> "반려됨";
            case COMPLETED -> "종료";
        };
    }

    private static Color color(TestStatus status) {
        return switch (status) {
            case WAITING -> COLOR_WAITING;
            case IN_PROGRESS -> COLOR_IN_PROGRESS;
            case REJECTED -> COLOR_REJECTED;
            case COMPLETED -> COLOR_COMPLETED;
        };
    }

    private static long epoch(LocalDateTime dateTime) {
        return dateTime.atZone(KST).toEpochSecond();
    }

    private static String pills(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "`-`";
        }
        return String.join(", ", values.stream().map(v -> "`" + v + "`").toList());
    }

    // 목록
    public static MessageEmbed listEmbed(TestStatus status, AdminTestListResponse response) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(statusLabel(status) + " 목록 조회")
                .setColor(color(status));

        if (response.tests().isEmpty()) {
            eb.setDescription(ZWSP + "\n조회된 테스트가 없습니다.");
            return eb.build();
        }

        StringBuilder sb = new StringBuilder(ZWSP).append("\n");
        List<AdminTestListItemResponse> items = response.tests();
        for (int i = 0; i < items.size(); i++) {
            AdminTestListItemResponse item = items.get(i);
            sb.append(String.format("**#%d %s**\n🏷️ %s · 🕒 <t:%d:R> · 💰 `%d`",
                    item.testId(), item.title(), pills(item.categories()),
                    epoch(item.createdAt()), item.reward()));
            if (i < items.size() - 1) {
                sb.append("\n\n");
            }
        }
        eb.setDescription(sb.toString());
        return eb.build();
    }

    public static int totalPages(AdminTestListResponse response) {
        if (response.size() <= 0) {
            return 1;
        }
        return (int) Math.max(1, Math.ceil((double) response.totalCount() / response.size()));
    }

    public static List<ActionRow> pageButtons(TestStatus status, AdminTestListResponse response) {
        int page = response.page();
        int total = totalPages(response);
        Button prev = Button.secondary(AdminBotCustomIds.list(status, page - 1), "이전")
                .withEmoji(Emoji.fromUnicode("◀️"));
        Button indicator = Button.secondary("admin:noop", page + " / " + total).asDisabled();
        Button next = Button.secondary(AdminBotCustomIds.list(status, page + 1), "다음")
                .withEmoji(Emoji.fromUnicode("▶️"));
        if (page <= 1) {
            prev = prev.asDisabled();
        }
        if (page >= total) {
            next = next.asDisabled();
        }
        return List.of(ActionRow.of(prev, indicator, next));
    }

    // 상세, 결과
    public static MessageEmbed detailEmbed(AdminTestDetailResponse d) {
        return new EmbedBuilder()
                .setTitle(String.format("%s · #%d %s", statusLabel(d.testStatus()), d.testId(), d.title()))
                .setColor(color(d.testStatus()))
                .setThumbnail(firstImageOrNull(d))
                .setDescription(detailBody(d, d.imageUrls() == null ? 0 : d.imageUrls().size(), null))
                .build();
    }

    public static MessageEmbed resultEmbed(AdminTestDetailResponse d) {
        String note = switch (d.testStatus()) {
            case IN_PROGRESS -> "이제 사용자에게 노출됩니다. 후반 검수 반려도 가능합니다.";
            case REJECTED -> "사용자 서비스에서 숨김 처리됩니다.";
            default -> null;
        };
        return new EmbedBuilder()
                .setTitle(String.format("%s · #%d %s", statusLabel(d.testStatus()), d.testId(), d.title()))
                .setColor(color(d.testStatus()))
                .setThumbnail(firstImageOrNull(d))
                .setDescription(detailBody(d, null, note))
                .build();
    }

    private static String detailBody(AdminTestDetailResponse d, Integer imageCount, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append(d.description() == null ? "" : d.description()).append("\n\n");
        sb.append(String.format("💰 리워드 | `%dP`\n", d.reward()));
        sb.append(String.format("🏷️ 카테고리 | %s\n", pills(d.categories())));
        sb.append(String.format("🕒 등록 | <t:%d:R>", epoch(d.createdAt())));
        if (imageCount != null && imageCount > 0) {
            sb.append(String.format("\n🖼️ 이미지 | `총 %d장`", imageCount));
        }
        if (d.rejectionReason() != null && !d.rejectionReason().isBlank()) {
            sb.append(String.format("\n🚫 사유 | `%s`", d.rejectionReason()));
        }
        if (note != null) {
            sb.append("\n\n> ").append(note);
        }
        return sb.toString();
    }

    private static String firstImageOrNull(AdminTestDetailResponse d) {
        return (d.imageUrls() == null || d.imageUrls().isEmpty()) ? null : d.imageUrls().get(0);
    }

    // 승인/반려/재승인 버튼
    public static List<ActionRow> actionButtons(long testId, TestStatus status) {
        List<Button> buttons = new ArrayList<>();
        switch (status) {
            case WAITING -> {
                buttons.add(Button.success(AdminBotCustomIds.approve(testId, status), "승인")
                        .withEmoji(Emoji.fromUnicode("✅")));
                buttons.add(Button.danger(AdminBotCustomIds.reject(testId, status), "반려")
                        .withEmoji(Emoji.fromUnicode("❌")));
            }
            case IN_PROGRESS -> buttons.add(Button.danger(AdminBotCustomIds.reject(testId, status), "반려")
                    .withEmoji(Emoji.fromUnicode("❌")));
            case REJECTED -> buttons.add(Button.success(AdminBotCustomIds.approve(testId, status), "재승인")
                    .withEmoji(Emoji.fromUnicode("🔁")));
            case COMPLETED -> {
                // 종료된 테스트는 액션 버튼 없음
            }
        }
        return buttons.isEmpty() ? List.of() : List.of(ActionRow.of(buttons));
    }

    // 승인/반려 처리 기록 로그
    public static String processLog(String heart, String actorName, TestStatus from, TestStatus to, String reason) {
        String verb = (to == TestStatus.REJECTED) ? "반려" : "승인";
        long now = epoch(LocalDateTime.now());
        StringBuilder sb = new StringBuilder(String.format("%s **%s** 님이 %s · `%s → %s` · <t:%d:f>",
                heart, actorName, verb, from.name(), to.name(), now));
        if (to == TestStatus.REJECTED && reason != null && !reason.isBlank()) {
            sb.append("\n> 사유: ").append(reason);
        }
        return sb.toString();
    }
}
