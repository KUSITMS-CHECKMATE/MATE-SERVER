package server.MATE.domain.report.excel.abtest;

public record AbTestReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        int totalCount,
        int versionACount,
        int versionBCount
) {
}
