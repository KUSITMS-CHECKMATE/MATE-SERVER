package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TestCloseProcessor testCloseProcessor;

    public void schedule(Long testId, LocalDateTime closedAt) {
        if (closedAt == null) {
            log.warn("테스트 {} 마감 시각이 없어 자동 종료 예약을 건너뜁니다", testId);
            return;
        }

        Instant triggerAt = closedAt.atZone(KST).toInstant();
        taskScheduler.schedule(() -> {
            try {
                testCloseProcessor.process(testId);
            } catch (Exception e) {
                log.error("테스트 {} 자동 종료 실패 - 자정 스케줄러에서 재처리됩니다", testId, e);
            }
        }, triggerAt);
        log.info("테스트 {} 자동 종료 예약 완료 - {}", testId, closedAt);
    }
}
