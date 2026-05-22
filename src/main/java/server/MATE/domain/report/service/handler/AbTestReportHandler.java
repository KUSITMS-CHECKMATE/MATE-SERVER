package server.MATE.domain.report.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
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
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForAbTest(answers));
        }
        return result;
    }

    private Map<String, Object> computeForAbTest(List<Answer> answers) {
        int aCount = 0;
        int bCount = 0;
        for (Answer answer : answers) {
            Object selected = answer.getAnswer().get("selected");
            if (selected == null) continue;
            if ("A".equals(String.valueOf(selected))) aCount++;
            else if ("B".equals(String.valueOf(selected))) bCount++;
        }
        int total = aCount + bCount;

        Map<String, Object> aMap = new LinkedHashMap<>();
        aMap.put("count", aCount);
        aMap.put("ratio", ReportHandlerUtils.toRatio(aCount, total));

        Map<String, Object> bMap = new LinkedHashMap<>();
        bMap.put("count", bCount);
        bMap.put("ratio", ReportHandlerUtils.toRatio(bCount, total));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("A", aMap);
        result.put("B", bMap);
        return result;
    }
}
