package server.MATE.domain.test.event;

import java.time.LocalDateTime;

public record TestCreatedEvent(
        Long testId,
        String title,
        Integer reward,
        LocalDateTime createdAt
) {
}
