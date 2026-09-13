package server.MATE.domain.test.event;

public record TestApprovedEvent(
        Long testId,
        String title
) {
}
