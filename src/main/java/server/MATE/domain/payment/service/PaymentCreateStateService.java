package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@RequiredArgsConstructor
public class PaymentCreateStateService {

    private final PaymentRepository paymentRepository;
    private final TestDraftRepository testDraftRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markCreated(Long paymentId,
                               Long draftId,
                               String orderNo,
                               Integer expectedAmount,
                               String payToken) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));

        payment.markCreated(payToken);
        draft.markPaymentCreated(orderNo, expectedAmount, payToken);
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long paymentId, Long draftId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));

        payment.markFailed();
        draft.markPaymentFailed();
    }
}
