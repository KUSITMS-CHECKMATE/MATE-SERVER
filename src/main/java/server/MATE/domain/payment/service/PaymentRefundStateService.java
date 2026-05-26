package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PaymentRefund;
import server.MATE.domain.payment.repository.PaymentRefundRepository;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;

@Service
@RequiredArgsConstructor
public class PaymentRefundStateService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markRefundPending(Long paymentId, Long makerId) {
        Payment payment = paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
        payment.markRefundPending();
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markRefunded(Long paymentId, Long makerId, String reason, TossPaymentRefundResponse result) {
        Payment payment = paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
        payment.markRefunded();

        paymentRefundRepository.save(PaymentRefund.builder()
                .paymentId(paymentId)
                .refundNo(result.refundNo())
                .reason(reason)
                .refundedAmount(result.refundedAmount())
                .transactionId(result.transactionId())
                .approvedAt(result.approvalTime())
                .build());
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markRefundFailed(Long paymentId, Long makerId) {
        Payment payment = paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
        payment.markRefundFailed();
        return payment;
    }
}
