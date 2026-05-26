package server.MATE.domain.report.excel;

public record AbTestReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        int totalCount,
        int versionACount,
        int versionBCount
) {
}
