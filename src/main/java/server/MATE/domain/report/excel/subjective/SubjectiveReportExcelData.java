package server.MATE.domain.report.excel.subjective;

import java.util.List;

public record SubjectiveReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<SubjectiveRespondentRow> respondents
) {
}
