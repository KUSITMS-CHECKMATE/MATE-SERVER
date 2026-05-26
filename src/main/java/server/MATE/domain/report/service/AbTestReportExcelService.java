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
import server.MATE.domain.report.excel.AbTestReportExcelData;
import server.MATE.domain.report.excel.AbTestReportExcelWriter;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AbTestReportExcelService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AbTestReportExcelWriter abTestReportExcelWriter;

    public TestReportExcelDownload export(Long testId, Long questionId, Long makerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        Question question = questionRepository.findByIdAndTestIdAndDeletedAtIsNull(questionId, testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));
        if (question.getQuestionType() != QuestionType.AB_TEST) {
            throw new BaseException(BaseErrorCode.REPORT_004);
        }

        List<Answer> answers = answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(questionId);
        int versionACount = 0;
        int versionBCount = 0;
        for (Answer answer : answers) {
            Object selected = answer.getAnswer().get("selected");
            if (selected == null) {
                continue;
            }
            if ("A".equals(String.valueOf(selected))) {
                versionACount++;
            } else if ("B".equals(String.valueOf(selected))) {
                versionBCount++;
            }
        }

        AbTestReportExcelData data = new AbTestReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                versionACount + versionBCount,
                versionACount,
                versionBCount
        );

        byte[] content = abTestReportExcelWriter.write(data);
        return new TestReportExcelDownload(content, buildFilename(testId, question.getSequence()));
    }

    private String buildFilename(Long testId, Long sequence) {
        return "mate-abtest-report-" + testId + "-q" + String.format("%02d", sequence) + ".xlsx";
    }
}
