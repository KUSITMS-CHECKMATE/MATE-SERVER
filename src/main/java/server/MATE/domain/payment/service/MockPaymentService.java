package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.dto.response.PaymentExecuteResponse;
import server.MATE.domain.payment.dto.response.PaymentRefundResponse;
import server.MATE.domain.payment.dto.response.PaymentStatusResponse;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;
import server.MATE.domain.testdraft.service.TestDraftPublishStateService;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.dto.request.TossPaymentCreateRequest;
import server.MATE.toss.dto.request.TossPaymentExecuteRequest;
import server.MATE.toss.dto.request.TossPaymentRefundRequest;
import server.MATE.toss.dto.response.TossPaymentCreateResponse;
import server.MATE.toss.dto.response.TossPaymentExecuteResponse;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;
import server.MATE.toss.gateway.TossPaymentGateway;

@Service
@RequiredArgsConstructor
public class MockPaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentGateway mockPaymentGateway;
    private final PaymentPrepareService paymentPrepareService;
    private final PaymentCreateStateService paymentCreateStateService;
    private final PaymentExecuteStateService paymentExecuteStateService;
    private final PaymentRefundStateService paymentRefundStateService;
    private final TestPublishService testPublishService;
    private final TestDraftPublishStateService testDraftPublishStateService;

    public PaymentCreateResponse createPayment(Long draftId, Long makerId, boolean isTestPayment) {
        PaymentPrepareService.PaymentPreparation preparation =
                paymentPrepareService.prepare(draftId, makerId, isTestPayment);
        if (preparation.hasExistingResponse()) {
            return preparation.existingResponse();
        }

        try {
            TossPaymentCreateResponse result = mockPaymentGateway.createPayment(
                    new TossPaymentCreateRequest(
                            preparation.orderNo(),
                            preparation.amount(),
                            preparation.isTestPayment()
                    )
            );

            paymentCreateStateService.markCreated(
                    preparation.paymentId(),
                    preparation.draftId(),
                    preparation.orderNo(),
                    preparation.amount(),
                    result.payToken()
            );

            return new PaymentCreateResponse(
                    preparation.paymentId(),
                    preparation.draftId(),
                    preparation.orderNo(),
                    preparation.amount(),
                    result.payToken(),
                    preparation.isTestPayment()
            );
        } catch (RuntimeException e) {
            paymentCreateStateService.markFailed(preparation.paymentId(), preparation.draftId());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PaymentExecuteResponse executePayment(Long paymentId, Long makerId) {
        Payment payment = getOwnedPayment(paymentId, makerId);
        if (payment.getPayStatus() == server.MATE.domain.payment.entity.PayStatus.PAY_SUCCEEDED) {
            if (payment.getTestId() != null) {
                return toExecuteResponse(payment, payment.getTestId());
            }
            Long testId = publishAfterExecution(payment);
            return toExecuteResponse(payment, testId);
        }
        payment.validateReadyToExecute();

        TossPaymentExecuteResponse result = mockPaymentGateway.executePayment(
                new TossPaymentExecuteRequest(payment.getPayToken(), payment.getOrderNo(), payment.getIsTestPayment())
        );

        int paidAmount = result.paidAmount() > 0 ? result.paidAmount() : payment.getAmount();

        Payment succeededPayment = paymentExecuteStateService.markSucceeded(
                payment.getId(),
                makerId,
                result.transactionId(),
                paidAmount,
                result.payMethod(),
                result.accountBankCode(),
                result.cardCompanyCode(),
                result.approvalTime()
        );

        Long testId = publishAfterExecution(succeededPayment);
        return toExecuteResponse(succeededPayment, testId);
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
        paymentRefundStateService.markRefundPending(paymentId, makerId);

        try {
            TossPaymentRefundResponse result = mockPaymentGateway.refundPayment(
                    new TossPaymentRefundRequest(payment.getPayToken(), reason, payment.getIsTestPayment())
            );

            Payment refundedPayment = paymentRefundStateService.markRefunded(paymentId, makerId, reason, result);
            return new PaymentRefundResponse(
                    refundedPayment.getId(),
                    result.refundNo(),
                    result.refundedAmount(),
                    result.transactionId(),
                    result.payToken(),
                    refundedPayment.getPayStatus(),
                    result.approvalTime()
            );
        } catch (RuntimeException e) {
            paymentRefundStateService.markRefundFailed(paymentId, makerId);
            throw e;
        }
    }

    private Payment getOwnedPayment(Long paymentId, Long makerId) {
        return paymentRepository.findByIdAndMakerId(paymentId, makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
    }

    private Long publishAfterExecution(Payment payment) {
        try {
            return testPublishService.publish(payment.getId());
        } catch (RuntimeException e) {
            testDraftPublishStateService.markPublishFailed(payment.getDraftId());
            throw e;
        }
    }

    private PaymentExecuteResponse toExecuteResponse(Payment payment, Long testId) {
        return new PaymentExecuteResponse(
                payment.getId(),
                payment.getDraftId(),
                testId,
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
}
