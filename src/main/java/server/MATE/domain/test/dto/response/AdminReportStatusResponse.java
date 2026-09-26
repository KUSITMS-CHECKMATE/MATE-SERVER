package server.MATE.domain.test.dto.response;

import server.MATE.domain.test.entity.ReportStatus;

public record AdminReportStatusResponse(Long testId, ReportStatus reportStatus) {
}
