package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.FiveSecondOption;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.FiveSecondRepository;
import server.MATE.domain.report.service.ReportHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FiveSecondReportHandler implements ReportHandler {

    private final FiveSecondRepository fiveSecondRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, FiveSecond> fiveSecondMap = fiveSecondRepository.findAllByIdIn(questionIds).stream()
                .collect(Collectors.toMap(FiveSecond::getId, f -> f));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            FiveSecond fiveSecond = fiveSecondMap.get(question.getId());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), fiveSecond.isObjective()
                    ? computeObjective(fiveSecond, answers)
                    : computeSubjective(answers));
        }
        return result;
    }

    private Map<String, Object> computeObjective(FiveSecond fiveSecond, List<Answer> answers) {
        Map<Long, Integer> countByOptionId = new LinkedHashMap<>();
        for (FiveSecondOption option : fiveSecond.getOptions()) {
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
        List<Map<String, Object>> options = fiveSecond.getOptions().stream()
                .sorted(Comparator.comparingInt((FiveSecondOption o) -> countByOptionId.getOrDefault(o.getId(), 0)).reversed())
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
        if (Boolean.TRUE.equals(fiveSecond.getIsOther())) {
            result.put("aiSummary", "AI 요약 준비 중입니다.");
            result.put("topAnswers", ReportHandlerUtils.topAnswers(otherTexts));
            result.put("otherTexts", otherTexts);
        }
        return result;
    }

    private Map<String, Object> computeSubjective(List<Answer> answers) {
        List<String> texts = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .map(a -> (String) a.getAnswer().get("text"))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiSummary", "AI 요약 준비 중입니다.");
        result.put("topAnswers", ReportHandlerUtils.topAnswers(texts));
        result.put("texts", texts);
        return result;
    }
}
