package server.MATE.domain.report.excel;

import java.util.List;

public record ObjectiveReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<ObjectiveRespondentRow> respondents,
        List<ObjectiveOptionStatRow> optionStats,
        int totalResponses
) {
}
