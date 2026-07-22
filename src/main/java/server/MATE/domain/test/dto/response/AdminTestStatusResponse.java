package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.TestStatus;

public record AdminTestStatusResponse(
        Long testId,
        TestStatus testStatus
) {
}
