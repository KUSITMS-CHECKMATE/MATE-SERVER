package server.MATE.domain.report.excel;

import server.MATE.domain.question.dto.response.QuestionSummaryItem;

import java.util.List;

public record TestReportExcelData(
        String testTitle,
        String testDescription,
        String testPeriod,
        int targetParticipantCount,
        List<QuestionSummaryItem> questions
) {
}
