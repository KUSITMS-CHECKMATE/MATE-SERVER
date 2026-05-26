package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.FiveSecondObjectiveReportExcelData;
import server.MATE.domain.report.excel.FiveSecondObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.FiveSecondOptionStatRow;
import server.MATE.domain.report.excel.FiveSecondRespondentRow;
import server.MATE.domain.report.excel.FiveSecondSubjectiveReportExcelData;
import server.MATE.domain.report.excel.FiveSecondSubjectiveReportExcelWriter;
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
public class FiveSecondReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final FiveSecondRepository fiveSecondRepository;
    private final FiveSecondObjectiveReportExcelWriter fiveSecondObjectiveReportExcelWriter;
    private final FiveSecondSubjectiveReportExcelWriter fiveSecondSubjectiveReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009
        );
        FiveSecond fiveSecond = fiveSecondRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        String filename = buildFilename(testId, question.getSequence());

        if (fiveSecond.isObjective()) {
            FiveSecondObjectiveReportExcelData data = prepareObjectiveData(testId, questionId, makerId);
            byte[] content = fiveSecondObjectiveReportExcelWriter.write(data);
            return new TestReportExcelDownload(content, filename);
        }

        FiveSecondSubjectiveReportExcelData data = prepareSubjectiveData(testId, questionId, makerId);
        byte[] content = fiveSecondSubjectiveReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, filename);
    }

    public FiveSecondObjectiveReportExcelData prepareObjectiveData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009
        );
        FiveSecond fiveSecond = fiveSecondRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);

        List<FiveSecondOption> options = fiveSecond.getOptions().stream()
                .sorted(Comparator.comparingInt(FiveSecondOption::getSequence))
                .toList();
        Map<Long, String> optionContentById = options.stream()
                .collect(Collectors.toMap(
                        FiveSecondOption::getId,
                        FiveSecondOption::getContent,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        return new FiveSecondObjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                buildObjectiveRespondentRows(answers, optionContentById),
                ReportExcelResultMapper.toFiveSecondOptionStats(options, reportResult),
                answers.size()
        );
    }

    public FiveSecondSubjectiveReportExcelData prepareSubjectiveData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009
        );
        reportExcelExportSupport.requireReportResult(testId, questionId);

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        return new FiveSecondSubjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                buildSubjectiveRespondentRows(answers)
        );
    }

    private List<FiveSecondRespondentRow> buildObjectiveRespondentRows(
            List<Answer> answers,
            Map<Long, String> optionContentById
    ) {
        List<FiveSecondRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            List<String> selectedLabels = new ArrayList<>();
            for (Long optionId : ReportHandlerUtils.extractOptionIds(answer.getAnswer())) {
                selectedLabels.add(optionContentById.getOrDefault(optionId, "알 수 없는 선지"));
            }

            Object otherText = answer.getAnswer().get("otherText");
            if (otherText instanceof String text && !text.isBlank()) {
                selectedLabels.add(text.trim());
            }

            rows.add(new FiveSecondRespondentRow(
                    answer.getParticipationId(),
                    String.join(", ", selectedLabels)
            ));
        }
        return rows;
    }

    private List<FiveSecondRespondentRow> buildSubjectiveRespondentRows(List<Answer> answers) {
        List<FiveSecondRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            rows.add(new FiveSecondRespondentRow(
                    answer.getParticipationId(),
                    extractAnswerText(answer)
            ));
        }
        return rows;
    }

    private String extractAnswerText(Answer answer) {
        Object text = answer.getAnswer().get("text");
        if (text instanceof String value && !value.isBlank()) {
            return value.trim();
        }
        return "";
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-five-second-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
