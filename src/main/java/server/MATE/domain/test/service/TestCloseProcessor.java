package server.MATE.domain.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.payment.service.MockPaymentService;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestCloseProcessor {

    private static final String REFUND_REASON = "테스트 응답 미달로 인한 자동 환불";

    private final TestRepository testRepository;
    private final PaymentRepository paymentRepository;
    private final MockPaymentService mockPaymentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void process(Long testId) {
        Test test = testRepository.findActiveById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        long threshold = (long) Math.ceil(test.getGoalPpl() * 0.2);
        test.complete();

        if (test.getPplCount() < threshold) {
            log.info("테스트 {} 응답 미달({}명 / 기준 {}명) - 환불 처리", testId, test.getPplCount(), threshold);
            paymentRepository.findByTestId(testId).ifPresent(payment -> {
                try {
                    mockPaymentService.refundPayment(payment.getId(), payment.getMakerId(), REFUND_REASON);
                } catch (Exception e) {
                    log.error("테스트 {} 환불 처리 실패 - paymentId={}", testId, payment.getId(), e);
                }
            });
        } else {
            log.info("테스트 {} 리포트 집계 시작({}명 / 기준 {}명)", testId, test.getPplCount(), threshold);
            test.startReportAggregation();
            eventPublisher.publishEvent(new TestCompleteEvent(testId));
        }
    }
}
