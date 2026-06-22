package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void requestRefund(Long testId, String reason) {
        Payment payment = paymentRepository.findByTestId(testId).orElse(null);
        if (payment == null) {
            log.warn("환불 대상 결제 없음 - testId={}", testId);
            return;
        }
        if (payment.getPayStatus() != PayStatus.PAY_SUCCEEDED) {
            log.warn("환불 불가 상태 - paymentId={}, status={}", payment.getId(), payment.getPayStatus());
            return;
        }

        payment.requestRefund(reason);
        log.info("환불 요청 완료 - paymentId={}, testId={}, amount={}, reason={}",
                payment.getId(), testId, payment.getPaidAmount(), reason);
    }

    @Transactional
    public void completeRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));

        if (payment.getPayStatus() != PayStatus.REFUND_PENDING) {
            throw new BaseException(BaseErrorCode.PAYMENT_007);
        }

        payment.completeRefund();
        log.info("환불 완료 처리 - paymentId={}, orderId={}", payment.getId(), payment.getOrderNo());
    }
}
