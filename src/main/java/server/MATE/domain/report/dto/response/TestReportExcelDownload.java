package server.MATE.domain.report.dto.response;

public record TestReportExcelDownload(
        byte[] content,
        String filename
) {
}
