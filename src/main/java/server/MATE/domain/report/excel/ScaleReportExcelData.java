package server.MATE.domain.report.excel;

import java.util.List;

public record ScaleReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<ScaleRespondentRow> respondents,
        List<ScaleValueStatRow> valueStats,
        String average
) {
}
