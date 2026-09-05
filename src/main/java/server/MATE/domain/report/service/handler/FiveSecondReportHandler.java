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
import server.MATE.global.claude.SubjectiveAiService;

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
    private final SubjectiveAiService aiService;

    @Override
    public QuestionType supports() {
        return QuestionType.FIVE_SECOND;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        return compute(questions, answersByQuestionId, true);
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId, boolean includeAiAnalysis) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, FiveSecond> fiveSecondMap = fiveSecondRepository.findAllByIdIn(questionIds).stream()
                .collect(Collectors.toMap(FiveSecond::getId, f -> f));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            FiveSecond fiveSecond = fiveSecondMap.get(question.getId());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), fiveSecond.isObjective()
                    ? computeObjective(fiveSecond, answers, includeAiAnalysis)
                    : computeSubjective(answers, includeAiAnalysis));
        }
        return result;
    }

    private Map<String, Object> computeObjective(FiveSecond fiveSecond, List<Answer> answers, boolean includeAiAnalysis) {
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
            if (otherText != null && !otherText.isBlank()) otherTexts.add(otherText);
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
            appendAiResult(result, otherTexts, "otherTexts", includeAiAnalysis);
        }
        return result;
    }

    private Map<String, Object> computeSubjective(List<Answer> answers, boolean includeAiAnalysis) {
        List<String> allTexts = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .map(a -> (String) a.getAnswer().get("text"))
                .filter(t -> t != null && !t.isBlank())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        appendAiResult(result, allTexts, "texts", includeAiAnalysis);
        return result;
    }

    private void appendAiResult(Map<String, Object> result, List<String> texts, String rawTextsKey, boolean includeAiAnalysis) {
        if (!includeAiAnalysis) {
            result.put("aiSummary", null);
            result.put("clusters", List.of());
            result.put(rawTextsKey, ReportHandlerUtils.sampleTexts(texts));
            return;
        }

        if (texts.size() < aiService.getMinResponseThreshold()) {
            result.put("aiSummary", null);
            result.put("clusters", List.of());
            result.put(rawTextsKey, texts);
            return;
        }

        aiService.analyze(texts).ifPresentOrElse(
                aiResult -> {
                    result.put("aiSummary", aiResult.aiSummary());
                    result.put("clusters", aiResult.toClusterMaps());
                    result.put(rawTextsKey, ReportHandlerUtils.sampleTexts(texts));
                },
                () -> {
                    result.put("aiSummary", null);
                    result.put("clusters", ReportHandlerUtils.buildClusters(texts));
                    result.put(rawTextsKey, ReportHandlerUtils.sampleTexts(texts));
                }
        );
    }
}
