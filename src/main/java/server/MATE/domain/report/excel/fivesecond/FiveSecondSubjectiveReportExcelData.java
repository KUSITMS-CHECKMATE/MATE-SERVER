package server.MATE.domain.report.excel.fivesecond;

import java.util.List;

public record FiveSecondSubjectiveReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<FiveSecondRespondentRow> respondents
) {
}
