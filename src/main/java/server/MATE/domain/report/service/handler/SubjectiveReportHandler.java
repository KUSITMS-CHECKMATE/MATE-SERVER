package server.MATE.domain.report.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.service.ReportHandler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SubjectiveReportHandler implements ReportHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForSubjective(answers));
        }
        return result;
    }

    private Map<String, Object> computeForSubjective(List<Answer> answers) {
        List<String> allTexts = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .map(a -> (String) a.getAnswer().get("text"))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiSummary", "AI 요약 준비 중입니다.");
        result.put("clusters", ReportHandlerUtils.buildClusters(allTexts));
        result.put("texts", ReportHandlerUtils.sampleTexts(allTexts));
        return result;
    }
}
