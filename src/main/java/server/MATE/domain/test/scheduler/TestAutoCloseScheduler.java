package server.MATE.domain.test.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.MATE.domain.test.service.TestAutoCloseService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestAutoCloseScheduler {

    private final TestAutoCloseService testAutoCloseService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void autoCloseExpiredTests() {
        log.info("테스트 자동 종료 스케줄러 실행");
        testAutoCloseService.closeExpiredTests();
    }
}
