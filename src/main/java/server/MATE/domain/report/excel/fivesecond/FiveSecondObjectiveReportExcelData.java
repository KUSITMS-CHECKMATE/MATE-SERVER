package server.MATE.domain.report.excel.fivesecond;

import java.util.List;

public record FiveSecondObjectiveReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<FiveSecondRespondentRow> respondents,
        List<FiveSecondOptionStatRow> optionStats,
        int totalResponses
) {
}
