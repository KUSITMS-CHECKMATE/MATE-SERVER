package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.fivesecond.FiveSecondObjectiveReportExcelData;
import server.MATE.domain.report.excel.fivesecond.FiveSecondRespondentRow;
import server.MATE.domain.report.excel.fivesecond.FiveSecondSubjectiveReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.domain.report.service.handler.ReportHandlerUtils;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FiveSecondReportExcelService {

    public FiveSecondObjectiveReportExcelData prepareObjectiveData(ReportExcelExportContext context, Long questionId) {
        Question question = context.requireQuestion(
                questionId, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009
        );
        FiveSecond fiveSecond = context.requireFiveSecond(questionId);
        if (!fiveSecond.isObjective()) {
            throw new BaseException(BaseErrorCode.REPORT_009);
        }
        Map<String, Object> reportResult = context.requireReportResult(questionId, QuestionType.OBJECTIVE);

        Map<Long, String> optionContentById = ReportExcelResultMapper.buildOptionContentFromReport(reportResult);

        List<Answer> answers = context.answers(questionId);
        return new FiveSecondObjectiveReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                buildObjectiveRespondentRows(answers, optionContentById),
                ReportExcelResultMapper.toFiveSecondOptionStats(reportResult),
                answers.size()
        );
    }

    public FiveSecondSubjectiveReportExcelData prepareSubjectiveData(ReportExcelExportContext context, Long questionId) {
        Question question = context.requireQuestion(
                questionId, QuestionType.FIVE_SECOND, BaseErrorCode.REPORT_009
        );
        FiveSecond fiveSecond = context.requireFiveSecond(questionId);
        if (fiveSecond.isObjective()) {
            throw new BaseException(BaseErrorCode.REPORT_009);
        }
        context.assertReportExists(questionId);

        List<Answer> answers = context.answers(questionId);
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
}
