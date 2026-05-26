package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.TreeTestPathStatRow;
import server.MATE.domain.report.excel.TreeTestReportExcelData;
import server.MATE.domain.report.excel.TreeTestReportExcelWriter;
import server.MATE.domain.report.excel.TreeTestRespondentRow;
import server.MATE.global.common.exception.BaseErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TreeTestReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final TreeTestRepository treeTestRepository;
    private final TreeTestReportExcelWriter treeTestReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.TREE_TEST, BaseErrorCode.REPORT_008
        );
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);

        Map<Long, TreeTest> nodeMap = treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(List.of(questionId))
                .stream()
                .collect(Collectors.toMap(TreeTest::getId, Function.identity()));

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        List<TreeTestRespondentRow> respondents = buildRespondentRows(nodeMap, answers);
        List<TreeTestPathStatRow> pathStats = ReportExcelResultMapper.toTreeTestPathStats(reportResult);
        int totalResponseCount = ReportExcelResultMapper.readTreeTestTotalResponseCount(reportResult);

        TreeTestReportExcelData data = new TreeTestReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                pathStats,
                totalResponseCount
        );

        byte[] content = treeTestReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
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

    @SuppressWarnings("unchecked")
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

    private String buildFilename(Long testId, Long sequence) {
        return "mate-tree-test-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
