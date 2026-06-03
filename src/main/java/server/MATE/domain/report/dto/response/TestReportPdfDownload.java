package server.MATE.domain.report.dto.response;

public record TestReportPdfDownload(
        byte[] data,
        String filename
) {
}
