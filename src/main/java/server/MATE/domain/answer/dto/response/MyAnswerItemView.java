package server.MATE.domain.answer.dto.response;

import java.time.LocalDateTime;

public record MyAnswerItemView(
        Long testId,
        String testName,
        LocalDateTime createdAt,
        Integer reward
) {
}
