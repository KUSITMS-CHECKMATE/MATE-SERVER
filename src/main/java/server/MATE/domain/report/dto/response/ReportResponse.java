package server.MATE.domain.report.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import server.MATE.domain.test.entity.ReportStatus;
import server.MATE.domain.test.entity.TestStatus;

import java.util.List;

@Schema(description = "테스트 리포트 전체 조회 응답")
public record ReportResponse(
        @Schema(description = "테스트 제목", example = "MATE 사용성 테스트")
        String title,

        @Schema(description = "테스트 상태 (IN_PROGRESS / COMPLETED)", example = "COMPLETED")
        TestStatus testStatus,

        @Schema(description = "리포트 집계 상태 (PENDING / IN_PROGRESS / COMPLETED / FAILED)", example = "COMPLETED")
        ReportStatus reportStatus,

        @Schema(description = "총 질문 수", example = "5")
        int questionCount,

        @Schema(description = "총 참여자 수", example = "12")
        Long participantCount,

        @Schema(description = "목표 인원 대비 달성률 (0~1)", example = "0.24")
        double achievementRate,

        @Schema(description = "질문별 집계 결과 (reportStatus가 COMPLETED이거나, testStatus가 IN_PROGRESS이고 응답률 50% 이상일 때 포함)")
        List<ReportItem> reports
) {
}
