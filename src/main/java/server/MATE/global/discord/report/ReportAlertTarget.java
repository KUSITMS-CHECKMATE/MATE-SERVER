package server.MATE.global.discord.report;

import server.MATE.domain.test.entity.Test;

import java.time.LocalDateTime;

// 리포트 알림에 필요한 테스트 정보 스냅샷. 조회 실패 시 ID만 보관
public record ReportAlertTarget(
        Long testId,
        String title,
        String description,
        long pplCount,
        LocalDateTime closedAt
) {
    public static ReportAlertTarget from(Test test) {
        return new ReportAlertTarget(test.getId(), test.getTitle(), test.getDescription(), test.getPplCount(), test.getClosedAt());
    }

    public static ReportAlertTarget idOnly(Long testId) {
        return new ReportAlertTarget(testId, null, null, 0L, null);
    }

    public String label() {
        return title == null ? "#" + testId : "#" + testId + " " + title;
    }

    public boolean hasDetail() {
        return title != null;
    }
}
