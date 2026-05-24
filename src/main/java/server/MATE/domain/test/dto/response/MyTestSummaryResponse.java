package server.MATE.domain.test.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

@Schema(description = "내가 생성한 테스트 목록 노출용 요약")
public record MyTestSummaryResponse(
        @Schema(description = "진행 상태", example = "IN_PROGRESS")
        TestStatus testStatus,
        @Schema(description = "테스트 제목", example = "승인된테스트1")
        String title,
        @Schema(description = "현재 참여 인원", example = "12")
        Long pplCount
) {
    public static MyTestSummaryResponse from(Test test) {
        return new MyTestSummaryResponse(
                test.getTestStatus(),
                test.getTitle(),
                test.getPplCount()
        );
    }
}
