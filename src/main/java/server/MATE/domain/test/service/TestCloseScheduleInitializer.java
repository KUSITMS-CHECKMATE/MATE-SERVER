package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseScheduleInitializer implements ApplicationRunner {

    private final TestRepository testRepository;
    private final TestCloseScheduler testCloseScheduler;

    @Override
    public void run(ApplicationArguments args) {
        List<Test> inProgressTests = testRepository.findByTestStatusAndDeletedAtIsNull(TestStatus.IN_PROGRESS);

        log.info("진행 중 테스트 자동 종료 예약 복원 대상: {}개", inProgressTests.size());

        for (Test test : inProgressTests) {
            if (test.getClosedAt() != null) {
                testCloseScheduler.schedule(test.getId(), test.getClosedAt());
            }
        }
    }
}
