package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.report.service.ReportHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScaleReportHandler implements ReportHandler {

    private final ScaleRepository scaleRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.SCALE;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, Scale> scaleMap = scaleRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Scale::getId, s -> s));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            Scale scale = scaleMap.get(question.getId());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForScale(scale, answers));
        }
        return result;
    }

    private Map<String, Object> computeForScale(Scale scale, List<Answer> answers) {
        int range = scale.getRange();
        int[] counts = new int[range + 1];
        int total = 0;
        long sum = 0;

        for (Answer answer : answers) {
            if (!(answer.getAnswer().get("value") instanceof Number number)) continue;
            int value = number.intValue();
            if (value >= 1 && value <= range) {
                counts[value]++;
                sum += value;
                total++;
            }
        }

        double average = total == 0 ? 0.0 : Math.round(sum * 10.0 / total) / 10.0;

        List<Map<String, Object>> distribution = new ArrayList<>();
        int mostVotedScore = 0;
        int maxCount = 0;
        for (int i = 1; i <= range; i++) {
            distribution.add(Map.of("score", i, "count", counts[i]));
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                mostVotedScore = i;
            }
        }

        Map<String, Object> endValue = new LinkedHashMap<>();
        endValue.put("minLabel", scale.getMinLabel());
        endValue.put("maxLabel", scale.getMaxLabel());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("average", average);
        result.put("mostVoted", mostVotedScore);
        result.put("endValue", endValue);
        result.put("distribution", distribution);
        return result;
    }
}
