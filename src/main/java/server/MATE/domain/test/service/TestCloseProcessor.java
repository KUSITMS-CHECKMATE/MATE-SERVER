package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.policy.RefundPolicy;
import server.MATE.domain.payment.service.RefundService;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseProcessor {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 예약 작업이 마감 시각보다 ms 단위로 일찍 실행돼도 정상 마감되도록 두는 여유
    private static final Duration EARLY_FIRE_TOLERANCE = Duration.ofMinutes(1);

    private final TestRepository testRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RefundPolicy refundPolicy;
    private final RefundService refundService;
    private final Clock clock;

    @Transactional
    public void process(Long testId) {
        Test test = testRepository.findByIdForUpdate(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        // 재개 전에 걸어둔 옛 예약이 새 마감보다 먼저 닫는 것 방지
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        if (now.plus(EARLY_FIRE_TOLERANCE).isBefore(test.getClosedAt())) {
            log.info("테스트 {} 마감 전 예약 실행 스킵 - closedAt={}, now={}", testId, test.getClosedAt(), now);
            return;
        }
        processClose(test);
    }

    void processClose(Test test) {
        if (test.getTestStatus() != TestStatus.IN_PROGRESS) {
            log.warn("테스트 {} 처리 스킵 - 현재 상태: {}", test.getId(), test.getTestStatus());
            return;
        }

        long threshold = (long) Math.ceil(test.getGoalPpl() * 0.2);
        test.complete();

        if (test.getPplCount() >= threshold) {
            log.info("테스트 {} 리포트 집계 시작({}명 / 기준 {}명)", test.getId(), test.getPplCount(), threshold);
            test.startReportAggregation();
            eventPublisher.publishEvent(new TestCompleteEvent(test.getId()));
        } else if (refundPolicy.isEligibleForRefund(test)) {
            log.info("테스트 {} 응답 미달({}명 / 기준 {}명) - 100% 환불 요청",
                    test.getId(), test.getPplCount(), threshold);
            refundService.requestRefund(test.getId(), "목표 달성률 20% 미만 자동 종료");
        } else {
            log.info("테스트 {} 응답 미달({}명 / 기준 {}명) - 환불 불가({})",
                    test.getId(), test.getPplCount(), threshold, describeRefundBlockReason(test));
        }
    }

    private String describeRefundBlockReason(Test test) {
        if (test.isClosedByMaker()) return "수동 종료";
        if (test.isRefundWaived()) return "진행 의사 선택";
        return "기타";
    }
}
