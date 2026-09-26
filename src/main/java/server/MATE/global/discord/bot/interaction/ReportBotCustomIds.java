package server.MATE.global.discord.bot.interaction;

// 리포트 관리 채널 버튼 ID 규칙. 테스트 관리 채널(admin:)과 분리용
public final class ReportBotCustomIds {

    public static final String PREFIX = "report";
    public static final String REAGGREGATE = "reaggregate";
    private static final String SEP = ":";

    private ReportBotCustomIds() {
    }

    public static String reaggregate(long testId) {
        return PREFIX + SEP + REAGGREGATE + SEP + testId;
    }

    public static boolean isReport(String customId) {
        return customId != null && customId.startsWith(PREFIX + SEP);
    }

    public static boolean isReaggregate(String customId) {
        return customId != null && customId.startsWith(PREFIX + SEP + REAGGREGATE + SEP);
    }

    public static long parseTestId(String customId) {
        return Long.parseLong(customId.substring(customId.lastIndexOf(SEP) + 1));
    }
}
