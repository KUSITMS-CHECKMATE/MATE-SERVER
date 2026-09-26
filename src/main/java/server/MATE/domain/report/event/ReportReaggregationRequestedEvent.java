package server.MATE.domain.report.event;

// 재집계 요청 알림용. requester는 처리 기록 줄의 주어("💙 **소윤** 님이", "🛠️ 관리자 API로")
public record ReportReaggregationRequestedEvent(Long testId, String requester) {
}
