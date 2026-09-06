package server.MATE.domain.report.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.service.ReportHandler;
import server.MATE.global.claude.SubjectiveAiService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class SubjectiveReportHandler implements ReportHandler {

    private final SubjectiveAiService aiService;
    private final Executor claudeAnalysisExecutor;

    public SubjectiveReportHandler(
            SubjectiveAiService aiService,
            @Qualifier("claudeAnalysisExecutor") Executor claudeAnalysisExecutor
    ) {
        this.aiService = aiService;
        this.claudeAnalysisExecutor = claudeAnalysisExecutor;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        return compute(questions, answersByQuestionId, true);
    }

    @Override
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId, boolean includeAiAnalysis) {
        // 실시간 미리보기(computeLive)는 Claude 호출이 없는 경량 연산이라 전용 풀을 거치지 않고 동기 처리
        if (!includeAiAnalysis) {
            Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
            for (Question question : questions) {
                List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
                result.put(question.getId(), computeForSubjective(answers, false));
            }
            return result;
        }

        long startedAt = System.currentTimeMillis();

        List<CompletableFuture<Map.Entry<Long, Map<String, Object>>>> futures = questions.stream()
                .map(question -> CompletableFuture.supplyAsync(() -> {
                    List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
                    return Map.entry(question.getId(), computeForSubjective(answers, true));
                }, claudeAnalysisExecutor))
                .toList();

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (CompletableFuture<Map.Entry<Long, Map<String, Object>>> future : futures) {
            Map.Entry<Long, Map<String, Object>> entry = future.join();
            result.put(entry.getKey(), entry.getValue());
        }

        if (!questions.isEmpty()) {
            log.info("주관식 문항 {}개 AI 분석 완료, 소요시간 {}ms", questions.size(), System.currentTimeMillis() - startedAt);
        }

        return result;
    }

    private Map<String, Object> computeForSubjective(List<Answer> answers, boolean includeAiAnalysis) {
        List<String> allTexts = answers.stream()
                .sorted(Comparator.comparing(Answer::getCreatedAt))
                .map(a -> (String) a.getAnswer().get("text"))
                .filter(t -> t != null && !t.isBlank())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();

        if (!includeAiAnalysis) {
            result.put("aiSummary", null);
            result.put("clusters", List.of());
            result.put("texts", ReportHandlerUtils.sampleTexts(allTexts));
            return result;
        }

        if (allTexts.size() < aiService.getMinResponseThreshold()) {
            result.put("aiSummary", null);
            result.put("clusters", List.of());
            result.put("texts", allTexts);
            return result;
        }

        aiService.analyze(allTexts).ifPresentOrElse(
                aiResult -> {
                    result.put("aiSummary", aiResult.aiSummary());
                    result.put("clusters", aiResult.toClusterMaps());
                    result.put("texts", ReportHandlerUtils.sampleTexts(allTexts));
                },
                () -> {
                    result.put("aiSummary", null);
                    result.put("clusters", ReportHandlerUtils.buildClusters(allTexts));
                    result.put("texts", ReportHandlerUtils.sampleTexts(allTexts));
                }
        );
        return result;
    }
}
