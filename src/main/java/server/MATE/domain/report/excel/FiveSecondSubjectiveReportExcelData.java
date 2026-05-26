package server.MATE.domain.report.excel;

import java.util.List;

public record FiveSecondSubjectiveReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<FiveSecondRespondentRow> respondents
) {
}
