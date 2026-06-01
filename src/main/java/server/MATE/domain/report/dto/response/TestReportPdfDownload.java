package server.MATE.domain.report.dto.response;

public record TestReportPdfDownload(
        String downloadUrl,
        String filename
) {
}
