package server.MATE.global.discord.bot.interaction;

import server.MATE.domain.test.entity.TestStatus;

/**
 * 버튼/모달 custom id 규약.
 * - 승인 버튼:  admin:approve:{testId}:{fromStatus}
 * - 반려 버튼:  admin:reject:{testId}:{fromStatus}   (누르면 모달을 띄운다)
 * - 반려 모달:  admin:rejectModal:{testId}:{fromStatus}  (입력창 id = reason)
 * - 목록 이동:  admin:list:{status}:{targetPage}
 */
public final class AdminBotCustomIds {

    public static final String PREFIX = "admin";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";
    public static final String REJECT_MODAL = "rejectModal";
    public static final String LIST = "list";
    public static final String REASON_INPUT = "reason";
    private static final String SEP = ":";

    private AdminBotCustomIds() {
    }

    public static String approve(long testId, TestStatus from) {
        return String.join(SEP, PREFIX, APPROVE, String.valueOf(testId), from.name());
    }

    public static String reject(long testId, TestStatus from) {
        return String.join(SEP, PREFIX, REJECT, String.valueOf(testId), from.name());
    }

    public static String rejectModal(long testId, TestStatus from) {
        return String.join(SEP, PREFIX, REJECT_MODAL, String.valueOf(testId), from.name());
    }

    public static String list(TestStatus status, int targetPage) {
        return String.join(SEP, PREFIX, LIST, status.name(), String.valueOf(targetPage));
    }

    public static boolean isAdmin(String customId) {
        return customId != null && customId.startsWith(PREFIX + SEP);
    }

    public static Parsed parse(String customId) {
        String[] parts = customId.split(SEP);
        return new Parsed(parts);
    }

    public record Parsed(String[] parts) {
        public String action() {
            return parts.length > 1 ? parts[1] : "";
        }

        public long testId() {
            return Long.parseLong(parts[2]);
        }

        public TestStatus fromStatus() {
            return TestStatus.valueOf(parts[3]);
        }

        public TestStatus listStatus() {
            return TestStatus.valueOf(parts[2]);
        }

        public int targetPage() {
            return Integer.parseInt(parts[3]);
        }
    }
}
