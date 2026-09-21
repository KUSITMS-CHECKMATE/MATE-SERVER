package server.MATE.domain.report.event;

public record ReportCompletedEvent(
        Long testId,
        Long makerId,
        String title
) {
}
