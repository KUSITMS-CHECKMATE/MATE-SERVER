package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.ObjectiveOptionStatRow;
import server.MATE.domain.report.excel.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.ObjectiveReportExcelWriter;
import server.MATE.domain.report.excel.ObjectiveRespondentRow;
import server.MATE.domain.report.service.handler.ReportHandlerUtils;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
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

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final AnswerRepository answerRepository;
    private final ObjectiveReportExcelWriter objectiveReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        Question question = questionRepository.findByIdAndTestIdAndDeletedAtIsNull(questionId, testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        if (question.getQuestionType() != QuestionType.OBJECTIVE) {
            throw new BaseException(BaseErrorCode.REPORT_002);
        }

        Objective objective = objectiveRepository.findWithOptionsById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<ObjectiveOption> options = objective.getOptions().stream()
                .sorted(Comparator.comparingInt(ObjectiveOption::getSequence))
                .toList();
        Map<Long, String> optionContentById = options.stream()
                .collect(Collectors.toMap(ObjectiveOption::getId, ObjectiveOption::getContent, (a, b) -> a, LinkedHashMap::new));

        List<Answer> answers = answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(questionId);
        List<ObjectiveRespondentRow> respondents = buildRespondentRows(answers, optionContentById);
        List<ObjectiveOptionStatRow> optionStats = buildOptionStats(options, answers);

        ObjectiveReportExcelData data = new ObjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                optionStats,
                answers.size()
        );

        byte[] content = objectiveReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
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

    private List<ObjectiveOptionStatRow> buildOptionStats(List<ObjectiveOption> options, List<Answer> answers) {
        Map<Long, Integer> countByOptionId = new LinkedHashMap<>();
        for (ObjectiveOption option : options) {
            countByOptionId.put(option.getId(), 0);
        }

        for (Answer answer : answers) {
            for (Long optionId : ReportHandlerUtils.extractOptionIds(answer.getAnswer())) {
                countByOptionId.merge(optionId, 1, Integer::sum);
            }
        }

        int total = answers.size();
        List<ObjectiveOptionStatRow> stats = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            ObjectiveOption option = options.get(index);
            int count = countByOptionId.getOrDefault(option.getId(), 0);
            stats.add(new ObjectiveOptionStatRow(
                    "선지 " + (index + 1),
                    count,
                    formatRatioPercent(count, total)
            ));
        }
        return stats;
    }

    private String formatRatioPercent(int count, int total) {
        if (total == 0) {
            return "-";
        }
        double percent = ReportHandlerUtils.toRatio(count, total) * 100;
        if (percent == Math.rint(percent)) {
            return String.valueOf((long) percent);
        }
        return String.valueOf(percent);
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-objective-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
