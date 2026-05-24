package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.dto.response.PaymentExecuteResponse;
import server.MATE.domain.payment.dto.response.PaymentRefundResponse;
import server.MATE.domain.payment.dto.response.PaymentStatusResponse;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.entity.PaymentRefund;
import server.MATE.domain.payment.repository.PaymentRefundRepository;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.dto.request.TossPaymentCreateRequest;
import server.MATE.toss.dto.request.TossPaymentExecuteRequest;
import server.MATE.toss.dto.request.TossPaymentRefundRequest;
import server.MATE.toss.dto.response.TossPaymentCreateResponse;
import server.MATE.toss.dto.response.TossPaymentExecuteResponse;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;
import server.MATE.toss.gateway.TossPaymentGateway;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MockPaymentService {

    private final TestDraftRepository testDraftRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final TossPaymentGateway mockPaymentGateway;
    private final PaymentAmountCalculator paymentAmountCalculator;

    public PaymentCreateResponse createPayment(Long draftId, Long makerId, boolean isTestPayment) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        draft.validateReadyForPayment();

        int amount = paymentAmountCalculator.calculate(draft.getGoalPpl(), draft.getReward());
        String orderNo = generateOrderNo(draft.getId());

        Payment payment = paymentRepository.save(Payment.builder()
                .draftId(draft.getId())
                .makerId(makerId)
                .orderNo(orderNo)
                .goalPpl(draft.getGoalPpl())
                .reward(draft.getReward())
                .amount(amount)
                .isTestPayment(isTestPayment)
                .build());

        TossPaymentCreateResponse result = mockPaymentGateway.createPayment(
                new TossPaymentCreateRequest(orderNo, amount, isTestPayment)
        );

        payment.markCreated(result.payToken());
        draft.markPaymentCreated(orderNo, amount, result.payToken());

        return new PaymentCreateResponse(
                payment.getId(),
                draft.getId(),
                orderNo,
                amount,
                result.payToken(),
                isTestPayment
        );
    }

    public PaymentExecuteResponse executePayment(Long paymentId, Long makerId) {
        Payment payment = getOwnedPayment(paymentId, makerId);
        payment.validateReadyToExecute();

        TossPaymentExecuteResponse result = mockPaymentGateway.executePayment(
                new TossPaymentExecuteRequest(payment.getPayToken(), payment.getOrderNo(), payment.getIsTestPayment())
        );

        payment.markSucceeded(
                result.transactionId(),
                result.paidAmount(),
                result.payMethod(),
                result.accountBankCode(),
                result.cardCompanyCode(),
                result.approvalTime()
        );

        return new PaymentExecuteResponse(
                payment.getId(),
                payment.getDraftId(),
                payment.getPayStatus(),
                payment.getOrderNo(),
                payment.getAmount(),
                payment.getPaidAmount(),
                payment.getPayToken(),
                payment.getTransactionId(),
                payment.getPayMethod(),
                payment.getApprovalTime()
        );
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(Long paymentId, Long makerId) {
        Payment payment = getOwnedPayment(paymentId, makerId);
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getDraftId(),
                payment.getTestId(),
                payment.getOrderNo(),
                payment.getPayToken(),
                payment.getPayStatus(),
                payment.getPayMethod(),
                payment.getAmount(),
                payment.getPaidAmount(),
                payment.getTransactionId(),
                payment.getApprovalTime()
        );
    }

    public PaymentRefundResponse refundPayment(Long paymentId, Long makerId, String reason) {
        Payment payment = getOwnedPayment(paymentId, makerId);
        payment.validateRefundable();
        payment.markRefundPending();

        TossPaymentRefundResponse result = mockPaymentGateway.refundPayment(
                new TossPaymentRefundRequest(payment.getPayToken(), reason, payment.getIsTestPayment())
        );

        paymentRefundRepository.save(PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundNo(result.refundNo())
                .reason(reason)
                .refundedAmount(result.refundedAmount())
                .transactionId(result.transactionId())
                .approvalTime(result.approvalTime())
                .build());

        payment.markRefunded();

        return new PaymentRefundResponse(
                payment.getId(),
                result.refundNo(),
                result.refundedAmount(),
                result.transactionId(),
                result.payToken(),
                payment.getPayStatus(),
                result.approvalTime()
        );
    }

    private TestDraft getOwnedDraft(Long draftId, Long makerId) {
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        if (!draft.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.DRAFT_002);
        }
        return draft;
    }

    private Payment getOwnedPayment(Long paymentId, Long makerId) {
        return paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
    }

    private String generateOrderNo(Long draftId) {
        return "draft-" + draftId + "-" + UUID.randomUUID();
    }
}
