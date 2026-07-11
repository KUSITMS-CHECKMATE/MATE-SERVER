package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseScheduleInitializer implements ApplicationRunner {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TestRepository testRepository;
    private final TestCloseScheduler testCloseScheduler;
    private final TestCloseProcessor testCloseProcessor;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        List<Test> inProgressTests = testRepository.findByTestStatusAndDeletedAtIsNull(TestStatus.IN_PROGRESS);

        log.info("진행 중 테스트 자동 종료 예약 복원 대상: {}개", inProgressTests.size());

        for (Test test : inProgressTests) {
            if (test.getClosedAt() == null) {
                continue;
            }
            if (!test.getClosedAt().isAfter(now)) {
                closeImmediately(test.getId());
                continue;
            }
            testCloseScheduler.schedule(test.getId(), test.getClosedAt());
        }
    }

    private void closeImmediately(Long testId) {
        try {
            testCloseProcessor.process(testId);
        } catch (Exception e) {
            log.error("테스트 {} 기한 만료 즉시 종료 실패 - 자정 스케줄러에서 재처리됩니다", testId, e);
        }
    }
}
