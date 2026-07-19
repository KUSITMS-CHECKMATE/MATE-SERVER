package server.MATE.domain.test.dto.request;

import jakarta.validation.constraints.Size;

public record RejectTestRequest(
        @Size(max = 500, message = "반려 사유는 500자 이하로 입력해주세요.")
        String reason
) {
}
