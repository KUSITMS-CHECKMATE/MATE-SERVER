package server.MATE.domain.report.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.claude.AiAnalysisOutcome;
import server.MATE.global.claude.AiAnalysisResult;
import server.MATE.global.claude.SubjectiveAiService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubjectiveReportHandlerTest {

    @Mock
    private SubjectiveAiService aiService;

    @Test
    @DisplayName("AI 실패는 질문 ID·원인으로 수집되고 결과는 폴백 요약 없음")
    void compute_collectsFailure() {
        SubjectiveReportHandler handler = new SubjectiveReportHandler(aiService, Runnable::run);
        given(aiService.getMinResponseThreshold()).willReturn(1);
        given(aiService.analyze(List.of("a1"))).willReturn(AiAnalysisOutcome.failure("E: boom"));
        given(aiService.analyze(List.of("b1"))).willReturn(AiAnalysisOutcome.success(new AiAnalysisResult("요약", List.of())));
        AiFailureCollector collector = new AiFailureCollector();

        Map<Long, Map<String, Object>> result = handler.compute(
                List.of(question(101L), question(102L)),
                Map.of(101L, List.of(answer(101L, "a1")), 102L, List.of(answer(102L, "b1"))),
                true, collector);

        assertThat(collector.attemptCount()).isEqualTo(2);
        assertThat(collector.failures()).containsExactly(new AiFailureCollector.AiFailure(101L, "E: boom"));
        assertThat(result.get(101L).get("aiSummary")).isNull();
        assertThat(result.get(102L).get("aiSummary")).isEqualTo("요약");
    }

    @Test
    @DisplayName("응답 수가 기준 미만이면 AI를 부르지 않고 시도로 세지 않는다")
    void compute_belowThreshold_notCounted() {
        SubjectiveReportHandler handler = new SubjectiveReportHandler(aiService, Runnable::run);
        given(aiService.getMinResponseThreshold()).willReturn(20);
        AiFailureCollector collector = new AiFailureCollector();

        handler.compute(List.of(question(101L)), Map.of(101L, List.of(answer(101L, "a1"))), true, collector);

        assertThat(collector.attemptCount()).isZero();
        verify(aiService, never()).analyze(anyList());
    }

    private Question question(Long id) {
        Question question = Question.builder().testId(1L).questionType(QuestionType.SUBJECTIVE)
                .title("q").description("d").sequence(id).build();
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Answer answer(Long questionId, String text) {
        Answer answer = Answer.builder().participationId(1L).questionId(questionId)
                .questionType(QuestionType.SUBJECTIVE).answer(Map.of("text", text)).build();
        ReflectionTestUtils.setField(answer, "createdAt", LocalDateTime.of(2026, 9, 27, 10, 0));
        return answer;
    }
}
