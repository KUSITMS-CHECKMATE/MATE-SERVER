package server.MATE.domain.test.dto.request;

import jakarta.validation.constraints.NotNull;
import server.MATE.domain.test.entity.TestStatus;

public record TestStatusUpdateRequest(
        @NotNull(message = "상태값은 필수입니다.")
        TestStatus status
) {
}
