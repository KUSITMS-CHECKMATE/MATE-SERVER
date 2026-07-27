package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPendingResolverService {

    private final PaymentRepository paymentRepository;
    private final TestPublishService testPublishService;

    public void resolveAll() {
        List<Payment> pending = paymentRepository.findTop100ByPayStatusAndTestIdIsNull(PayStatus.PAY_SUCCEEDED);
        log.info("PAYMENT PENDING 처리 대상: {}건", pending.size());

        for (Payment payment : pending) {
            try {
                testPublishService.publish(payment.getId());
                log.info("PAYMENT PENDING 처리 완료. paymentId={}", payment.getId());
            } catch (Exception e) {
                log.warn("PAYMENT PENDING 처리 실패. paymentId={}", payment.getId(), e);
            }
        }
    }
}
