package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Objective;
import server.MATE.domain.question.entity.ObjectiveOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.ObjectiveRepository;
import server.MATE.domain.report.service.ReportHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObjectiveReportHandler implements ReportHandler {

    private final ObjectiveRepository objectiveRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.OBJECTIVE;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, Objective> objectiveMap = objectiveRepository.findAllByIdIn(questionIds).stream()
                .collect(Collectors.toMap(Objective::getId, o -> o));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            Objective objective = objectiveMap.get(question.getId());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForObjective(objective, answers));
        }
        return result;
    }

    private Map<String, Object> computeForObjective(Objective objective, List<Answer> answers) {
        Map<Long, Integer> countByOptionId = new LinkedHashMap<>();
        for (ObjectiveOption option : objective.getOptions()) {
            countByOptionId.put(option.getId(), 0);
        }

        List<String> otherTexts = new ArrayList<>();
        for (Answer answer : answers) {
            for (Long optionId : ReportHandlerUtils.extractOptionIds(answer.getAnswer())) {
                countByOptionId.merge(optionId, 1, Integer::sum);
            }
            String otherText = (String) answer.getAnswer().get("otherText");
            if (otherText != null) otherTexts.add(otherText);
        }

        int total = answers.size();
        List<Map<String, Object>> options = objective.getOptions().stream()
                .sorted(Comparator.comparingInt((ObjectiveOption o) -> countByOptionId.getOrDefault(o.getId(), 0)).reversed())
                .map(option -> {
                    int count = countByOptionId.getOrDefault(option.getId(), 0);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("optionId", option.getId());
                    m.put("content", option.getContent());
                    m.put("count", count);
                    m.put("ratio", ReportHandlerUtils.toRatio(count, total));
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("options", options);
        if (objective.isOther()) {
            result.put("aiSummary", "AI 요약 준비 중입니다.");
            result.put("clusters", ReportHandlerUtils.buildClusters(otherTexts));
            result.put("texts", ReportHandlerUtils.sampleTexts(otherTexts));
        }
        return result;
    }
}
