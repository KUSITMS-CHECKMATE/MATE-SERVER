package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.TestStatus;

public record TestStatusUpdateResponse(
        Long testId,
        TestStatus testStatus
) {
}
