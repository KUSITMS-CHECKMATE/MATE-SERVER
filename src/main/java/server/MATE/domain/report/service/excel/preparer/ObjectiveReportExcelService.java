package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.report.excel.objective.ObjectiveOptionStatRow;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.objective.ObjectiveRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.domain.report.service.handler.ReportHandlerUtils;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ObjectiveReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final ObjectiveRepository objectiveRepository;

    public ObjectiveReportExcelData prepareData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.OBJECTIVE, BaseErrorCode.REPORT_002
        );
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);

        Objective objective = objectiveRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<ObjectiveOption> options = objective.getOptions().stream()
                .sorted(Comparator.comparingInt(ObjectiveOption::getSequence))
                .toList();
        Map<Long, String> optionContentById = options.stream()
                .collect(Collectors.toMap(ObjectiveOption::getId, ObjectiveOption::getContent, (a, b) -> a, LinkedHashMap::new));

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        List<ObjectiveRespondentRow> respondents = buildRespondentRows(answers, optionContentById);
        List<ObjectiveOptionStatRow> optionStats = ReportExcelResultMapper.toObjectiveOptionStats(options, reportResult);

        return new ObjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                optionStats,
                answers.size()
        );
    }

    private List<ObjectiveRespondentRow> buildRespondentRows(
            List<Answer> answers,
            Map<Long, String> optionContentById
    ) {
        List<ObjectiveRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            List<String> selectedLabels = new ArrayList<>();
            for (Long optionId : ReportHandlerUtils.extractOptionIds(answer.getAnswer())) {
                selectedLabels.add(optionContentById.getOrDefault(optionId, "알 수 없는 선지"));
            }

            Object otherText = answer.getAnswer().get("otherText");
            if (otherText instanceof String text && !text.isBlank()) {
                selectedLabels.add(text.trim());
            }

            rows.add(new ObjectiveRespondentRow(
                    answer.getParticipationId(),
                    String.join(", ", selectedLabels)
            ));
        }
        return rows;
    }
}
