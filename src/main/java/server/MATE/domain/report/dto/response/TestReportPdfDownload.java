package server.MATE.domain.report.dto.response;

public record TestReportPdfDownload(
        byte[] content,
        String filename
) {
}
