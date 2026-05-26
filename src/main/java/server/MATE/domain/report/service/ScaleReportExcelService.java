package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.ScaleReportExcelData;
import server.MATE.domain.report.excel.ScaleReportExcelWriter;
import server.MATE.domain.report.excel.ScaleRespondentRow;
import server.MATE.domain.report.excel.ScaleValueStatRow;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScaleReportExcelService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final ScaleRepository scaleRepository;
    private final AnswerRepository answerRepository;
    private final ScaleReportExcelWriter scaleReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        Question question = questionRepository.findByIdAndTestIdAndDeletedAtIsNull(questionId, testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        if (question.getQuestionType() != QuestionType.SCALE) {
            throw new BaseException(BaseErrorCode.REPORT_005);
        }

        Scale scale = scaleRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<Answer> answers = answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(questionId);
        int range = scale.getRange();
        int[] counts = new int[range + 1];
        int total = 0;
        long sum = 0;

        List<ScaleRespondentRow> respondents = new ArrayList<>();
        for (Answer answer : answers) {
            Integer value = extractScaleValue(answer);
            if (value == null) {
                continue;
            }
            respondents.add(new ScaleRespondentRow(answer.getParticipationId(), value));
            if (value >= 1 && value <= range) {
                counts[value]++;
                sum += value;
                total++;
            }
        }

        List<ScaleValueStatRow> valueStats = new ArrayList<>();
        for (int score = 1; score <= range; score++) {
            valueStats.add(new ScaleValueStatRow(
                    score,
                    counts[score],
                    formatRatioPercent(counts[score], total)
            ));
        }

        String average = formatAverage(total, sum);
        ScaleReportExcelData data = new ScaleReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                valueStats,
                average
        );

        byte[] content = scaleReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
    }

    private Integer extractScaleValue(Answer answer) {
        Object value = answer.getAnswer().get("value");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String formatRatioPercent(int count, int total) {
        if (total == 0) {
            return "0.00%";
        }
        double percent = count * 100.0 / total;
        return String.format("%.2f%%", percent);
    }

    private String formatAverage(int total, long sum) {
        if (total == 0) {
            return "0";
        }
        double average = Math.round(sum * 10.0 / total) / 10.0;
        if (average == Math.rint(average)) {
            return String.valueOf((long) average);
        }
        return String.valueOf(average);
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-scale-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
