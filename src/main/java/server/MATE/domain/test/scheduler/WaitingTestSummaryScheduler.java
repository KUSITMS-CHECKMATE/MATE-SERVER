package server.MATE.domain.test.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.MATE.domain.test.service.WaitingTestSummaryService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingTestSummaryScheduler {

    private final WaitingTestSummaryService waitingTestSummaryService;

    @Scheduled(cron = "0 0 10,16 * * *", zone = "Asia/Seoul")
    // 수 ms 만에 끝나는 작업이라 늦게 발동한 pod의 중복 발송 방지
    @SchedulerLock(name = "waitingTestSummaryScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void notifyLongWaitingTests() {
        log.info("[DISCORD] 검토 대기 요약 알림 스케줄러 실행");
        waitingTestSummaryService.notifyLongWaitingTests();
    }
}
