package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestAutoCloseService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TestRepository testRepository;
    private final TestCloseProcessor testCloseProcessor;
    private final Clock clock;

    public void closeExpiredTests() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        List<Test> expiredTests = testRepository.findExpiredInProgressTests(now);

        log.info("자동 종료 대상 테스트: {}개", expiredTests.size());

        for (Test test : expiredTests) {
            try {
                testCloseProcessor.process(test.getId());
            } catch (Exception e) {
                log.error("테스트 {} 자동 종료 실패", test.getId(), e);
            }
        }
    }
}
