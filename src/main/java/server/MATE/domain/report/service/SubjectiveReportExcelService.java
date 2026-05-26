package server.MATE.domain.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.SubjectiveReportExcelData;
import server.MATE.domain.report.excel.SubjectiveReportExcelWriter;
import server.MATE.domain.report.excel.SubjectiveRespondentRow;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubjectiveReportExcelService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final SubjectiveReportExcelWriter subjectiveReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        Question question = questionRepository.findByIdAndTestIdAndDeletedAtIsNull(questionId, testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
            throw new BaseException(BaseErrorCode.REPORT_003);
        }

        List<Answer> answers = answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(questionId);
        List<SubjectiveRespondentRow> respondents = buildRespondentRows(answers);

        SubjectiveReportExcelData data = new SubjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents
        );

        byte[] content = subjectiveReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
    }

    private List<SubjectiveRespondentRow> buildRespondentRows(List<Answer> answers) {
        List<SubjectiveRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            rows.add(new SubjectiveRespondentRow(
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
        return "mate-subjective-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
