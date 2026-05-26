package server.MATE.domain.report.excel.scale;

import java.util.List;

public record ScaleReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<ScaleRespondentRow> respondents,
        List<ScaleValueStatRow> valueStats,
        String average
) {
}
