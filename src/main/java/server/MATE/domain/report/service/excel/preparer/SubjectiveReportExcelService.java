package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.subjective.SubjectiveReportExcelData;
import server.MATE.domain.report.excel.subjective.SubjectiveRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.global.common.exception.BaseErrorCode;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubjectiveReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;

    public SubjectiveReportExcelData prepareData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.SUBJECTIVE, BaseErrorCode.REPORT_003
        );
        reportExcelExportSupport.requireReportResult(testId, questionId);

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        List<SubjectiveRespondentRow> respondents = buildRespondentRows(answers);

        return new SubjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents
        );
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
}
