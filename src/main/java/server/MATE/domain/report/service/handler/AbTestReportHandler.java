package server.MATE.domain.report.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.AbTestReportResult;
import server.MATE.domain.report.service.ReportHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AbTestReportHandler implements ReportHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.AB_TEST;
    }

    @Override
    public Map<Long, Object> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        Map<Long, Object> result = new LinkedHashMap<>();
        for (Question question : questions) {
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForAbTest(answers));
        }
        return result;
    }

    private AbTestReportResult computeForAbTest(List<Answer> answers) {
        int aCount = 0;
        int bCount = 0;
        for (Answer answer : answers) {
            Object selectedObj = answer.getAnswer().get("selected");
            if (selectedObj == null) continue;
            String selected = String.valueOf(selectedObj);
            if ("A".equals(selected)) aCount++;
            else if ("B".equals(selected)) bCount++;
        }
        int total = aCount + bCount;
        return new AbTestReportResult(
                aCount, ReportHandlerUtils.toPercentage(aCount, total),
                bCount, ReportHandlerUtils.toPercentage(bCount, total)
        );
    }
}
