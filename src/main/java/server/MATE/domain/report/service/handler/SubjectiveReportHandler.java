package server.MATE.domain.report.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.dto.response.SubjectiveReportResult;
import server.MATE.domain.report.service.ReportHandler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SubjectiveReportHandler implements ReportHandler {

    private static final int MAX_RESPONSES = 15;

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public Map<Long, Object> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        Map<Long, Object> result = new LinkedHashMap<>();
        for (Question question : questions) {
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForSubjective(answers));
        }
        return result;
    }

    SubjectiveReportResult computeForSubjective(List<Answer> answers) {
        List<String> responses = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .limit(MAX_RESPONSES)
                .map(a -> (String) a.getAnswer().get("text"))
                .toList();
        return new SubjectiveReportResult(responses);
    }
}
