package server.MATE.domain.test.event;

public record TestApprovedEvent(
        Long testId,
        Long makerId,
        String title
) {
}
