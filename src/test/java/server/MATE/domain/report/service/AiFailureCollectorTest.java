package server.MATE.domain.report.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.MATE.global.claude.AiAnalysisOutcome;
import server.MATE.global.claude.AiAnalysisResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

class AiFailureCollectorTest {

    @Test
    @DisplayName("성공과 실패를 모두 시도로 세고, 실패만 질문 ID 순으로 모은다")
    void record_countsAttemptsAndSortsFailures() {
        AiFailureCollector collector = new AiFailureCollector();

        collector.record(104L, AiAnalysisOutcome.failure("B"));
        collector.record(102L, AiAnalysisOutcome.success(new AiAnalysisResult("요약", List.of())));
        collector.record(101L, AiAnalysisOutcome.failure("A"));

        assertThat(collector.attemptCount()).isEqualTo(3);
        assertThat(collector.hasFailures()).isTrue();
        assertThat(collector.failures()).containsExactly(
                new AiFailureCollector.AiFailure(101L, "A"), new AiFailureCollector.AiFailure(104L, "B"));
    }

    @Test
    @DisplayName("여러 스레드가 동시에 실패를 넣어도 모두 모인다")
    void record_concurrentFailures_allCollected() {
        AiFailureCollector collector = new AiFailureCollector();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CompletableFuture.allOf(LongStream.rangeClosed(1, 200)
                    .mapToObj(id -> CompletableFuture.runAsync(() -> collector.record(id, AiAnalysisOutcome.failure("x")), pool))
                    .toArray(CompletableFuture[]::new)).join();
        } finally {
            pool.shutdown();
        }

        assertThat(collector.attemptCount()).isEqualTo(200);
        assertThat(collector.failures()).hasSize(200);
    }
}
