package server.MATE.domain.report.event;

// 리포트 집계 FAILED 확정 알림용. 예외 없는 불일치는 errorMessage로 설명함
public record ReportAggregationFailedEvent(Long testId, String cause, String errorMessage, Throwable exception) {

    public static final String STALE_AFTER_REOPEN = "재개 전 리포트만 남음";
    public static final String RETRY_EXHAUSTED = "재시도 3회 실패";
    public static final String NON_RETRYABLE = "재시도 제외 오류";

    public static ReportAggregationFailedEvent of(Long testId, String cause, Throwable exception) {
        return new ReportAggregationFailedEvent(testId, cause, null, exception);
    }

    public static ReportAggregationFailedEvent mismatch(Long testId, long questionCount, long reportCount) {
        return new ReportAggregationFailedEvent(
                testId,
                String.format("리포트 불일치 (질문 %d개, 리포트 %d개)", questionCount, reportCount),
                String.format("리포트 불일치: 질문 %d개, 리포트 %d개", questionCount, reportCount),
                null);
    }

    public static ReportAggregationFailedEvent mismatch(Long testId, long questionCount, long reportCount, Throwable exception) {
        return new ReportAggregationFailedEvent(
                testId,
                String.format("리포트 불일치 (질문 %d개, 리포트 %d개)", questionCount, reportCount),
                null,
                exception);
    }
}
