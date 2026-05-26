package server.MATE.domain.report.excel.cardsorting;

import java.util.List;

public record CardSortingReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<CardSortingRespondentRow> respondents,
        List<CardSortingCategoryStatRow> categoryStats
) {
}
