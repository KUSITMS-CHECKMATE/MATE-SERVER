package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.objective.ObjectiveReportExcelData;
import server.MATE.domain.report.excel.objective.ObjectiveRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.domain.report.service.handler.ReportHandlerUtils;
import server.MATE.global.common.exception.BaseErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ObjectiveReportExcelService {

    public ObjectiveReportExcelData prepareData(ReportExcelExportContext context, Long questionId) {
        Question question = context.requireQuestion(
                questionId, QuestionType.OBJECTIVE, BaseErrorCode.REPORT_002
        );
        Map<String, Object> reportResult = context.requireReportResult(questionId, QuestionType.OBJECTIVE);

        Map<Long, String> optionContentById = ReportExcelResultMapper.buildOptionContentFromReport(reportResult);

        List<Answer> answers = context.answers(questionId);
        List<ObjectiveRespondentRow> respondents = buildRespondentRows(answers, optionContentById);
        var optionStats = ReportExcelResultMapper.toObjectiveOptionStats(reportResult);

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
