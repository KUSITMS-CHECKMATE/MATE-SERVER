package server.MATE.domain.report.excel.treetest;

import java.util.List;

public record TreeTestReportExcelData(
        String questionNumberLabel,
        String questionTitle,
        List<TreeTestRespondentRow> respondents,
        List<TreeTestPathStatRow> pathStats,
        int totalResponseCount
) {
}
