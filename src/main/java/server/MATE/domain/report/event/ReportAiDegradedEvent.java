package server.MATE.domain.report.event;

import server.MATE.domain.report.service.handler.AiFailureCollector;

import java.util.List;

// 집계는 완료됐지만 AI 요약이 빠진 문항이 있을 때의 알림용
public record ReportAiDegradedEvent(Long testId, int attemptCount, List<AiFailureCollector.AiFailure> failures) {
}
