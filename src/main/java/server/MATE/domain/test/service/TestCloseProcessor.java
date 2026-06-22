package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseProcessor {

    private final TestRepository testRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void process(Long testId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        if (test.getTestStatus() != TestStatus.IN_PROGRESS) {
            log.warn("테스트 {} 처리 스킵 - 현재 상태: {}", testId, test.getTestStatus());
            return;
        }

        long threshold = (long) Math.ceil(test.getGoalPpl() * 0.2);
        test.complete();

        if (test.getPplCount() < threshold) {
            log.info("테스트 {} 응답 미달({}명 / 기준 {}명) - 환불 처리 필요", testId, test.getPplCount(), threshold);
            // TODO: 실결제 연동 후 환불 로직 구현
        } else {
            log.info("테스트 {} 리포트 집계 시작({}명 / 기준 {}명)", testId, test.getPplCount(), threshold);
            test.startReportAggregation();
            eventPublisher.publishEvent(new TestCompleteEvent(testId));
        }
    }
}
