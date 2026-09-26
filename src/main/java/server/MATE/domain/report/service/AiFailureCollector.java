package server.MATE.domain.report.service;

import server.MATE.global.claude.AiAnalysisOutcome;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

// 집계 1회 동안 AI 분석 시도·실패를 모으는 수집함. 주관식 병렬 실행 대응용 동시성 안전 구조
public class AiFailureCollector {

    private final AtomicInteger attemptCount = new AtomicInteger();
    private final ConcurrentLinkedQueue<AiFailure> failures = new ConcurrentLinkedQueue<>();

    public void record(Long questionId, AiAnalysisOutcome outcome) {
        attemptCount.incrementAndGet();
        if (!outcome.isSuccess()) {
            failures.add(new AiFailure(questionId, outcome.failureReason()));
        }
    }

    public int attemptCount() {
        return attemptCount.get();
    }

    public List<AiFailure> failures() {
        return failures.stream().sorted(Comparator.comparing(AiFailure::questionId)).toList();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public record AiFailure(Long questionId, String reason) {
    }
}
