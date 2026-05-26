package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.report.excel.treetest.TreeTestPathStatRow;
import server.MATE.domain.report.excel.treetest.TreeTestReportExcelData;
import server.MATE.domain.report.excel.treetest.TreeTestRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.global.common.exception.BaseErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TreeTestReportExcelService {

    public TreeTestReportExcelData prepareData(ReportExcelExportContext context, Long questionId) {
        Question question = context.requireQuestion(
                questionId, QuestionType.TREE_TEST, BaseErrorCode.REPORT_008
        );
        Map<String, Object> reportResult = context.requireReportResult(questionId, QuestionType.TREE_TEST);

        Map<Long, TreeTest> nodeMap = context.treeNodesByQuestionId(questionId);

        List<Answer> answers = context.answers(questionId);
        List<TreeTestRespondentRow> respondents = buildRespondentRows(nodeMap, answers);
        List<TreeTestPathStatRow> pathStats = ReportExcelResultMapper.toTreeTestPathStats(reportResult);
        int totalResponseCount = ReportExcelResultMapper.readTreeTestTotalResponseCount(reportResult);

        return new TreeTestReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                pathStats,
                totalResponseCount
        );
    }

    private List<TreeTestRespondentRow> buildRespondentRows(Map<Long, TreeTest> nodeMap, List<Answer> answers) {
        List<TreeTestRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            Long nodeId = extractNodeId(answer);
            if (nodeId == null) {
                continue;
            }
            TreeTest node = nodeMap.get(nodeId);
            String response = node != null ? node.getLabel() : String.valueOf(nodeId);
            rows.add(new TreeTestRespondentRow(
                    answer.getParticipationId(),
                    response,
                    extractPath(answer).size()
            ));
        }
        return rows;
    }

    private Long extractNodeId(Answer answer) {
        Object nodeIdObject = answer.getAnswer().get("nodeId");
        if (nodeIdObject instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private List<Long> extractPath(Answer answer) {
        Object pathObject = answer.getAnswer().get("path");
        if (!(pathObject instanceof List<?> rawPath)) {
            return List.of();
        }
        List<Long> path = new ArrayList<>();
        for (Object value : rawPath) {
            if (value instanceof Number number) {
                path.add(number.longValue());
            }
        }
        return path;
    }
}
