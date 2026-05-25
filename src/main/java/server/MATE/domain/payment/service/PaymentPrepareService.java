package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentPrepareService {

    private final TestDraftRepository testDraftRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAmountCalculator paymentAmountCalculator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentPreparation prepare(Long draftId, Long makerId, boolean isTestPayment) {
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        if (!draft.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.DRAFT_002);
        }
        draft.validateReadyForPayment();

        int amount = paymentAmountCalculator.calculate(draft.getGoalPpl(), draft.getReward());
        Payment existingPayment = paymentRepository.findByDraftId(draftId).orElse(null);
        if (existingPayment != null) {
            if (existingPayment.getPayStatus() == PayStatus.PAY_CREATED) {
                return PaymentPreparation.withExistingResponse(new PaymentCreateResponse(
                        existingPayment.getId(),
                        draft.getId(),
                        existingPayment.getOrderNo(),
                        existingPayment.getAmount(),
                        existingPayment.getPayToken(),
                        existingPayment.getIsTestPayment()
                ));
            }
            if (existingPayment.getPayStatus() == PayStatus.PAY_SUCCEEDED) {
                throw new BaseException(BaseErrorCode.PAYMENT_002);
            }
        }

        String orderNo = generateOrderNo(draft.getId());
        Payment payment = existingPayment == null
                ? paymentRepository.save(Payment.builder()
                .draftId(draft.getId())
                .makerId(makerId)
                .orderNo(orderNo)
                .goalPpl(draft.getGoalPpl())
                .reward(draft.getReward())
                .amount(amount)
                .isTestPayment(isTestPayment)
                .build())
                : resetPaymentForRetry(existingPayment, orderNo, draft.getGoalPpl(), draft.getReward(), amount, isTestPayment);

        return PaymentPreparation.forGatewayCall(
                payment.getId(),
                draft.getId(),
                orderNo,
                amount,
                isTestPayment
        );
    }

    private Payment resetPaymentForRetry(Payment payment,
                                         String orderNo,
                                         Integer goalPpl,
                                         Integer reward,
                                         Integer amount,
                                         boolean isTestPayment) {
        if (payment.getPayStatus() != PayStatus.PAY_FAILED
                && payment.getPayStatus() != PayStatus.REFUND_FAILED) {
            throw new BaseException(BaseErrorCode.PAYMENT_002);
        }
        payment.prepareForRetry(orderNo, goalPpl, reward, amount, isTestPayment);
        return payment;
    }

    private String generateOrderNo(Long draftId) {
        return "d-" + draftId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public record PaymentPreparation(
            Long paymentId,
            Long draftId,
            String orderNo,
            Integer amount,
            Boolean isTestPayment,
            PaymentCreateResponse existingResponse
    ) {
        static PaymentPreparation withExistingResponse(PaymentCreateResponse response) {
            return new PaymentPreparation(
                    response.paymentId(),
                    response.draftId(),
                    response.orderNo(),
                    response.amount(),
                    response.isTestPayment(),
                    response
            );
        }

        static PaymentPreparation forGatewayCall(Long paymentId,
                                                 Long draftId,
                                                 String orderNo,
                                                 Integer amount,
                                                 Boolean isTestPayment) {
            return new PaymentPreparation(paymentId, draftId, orderNo, amount, isTestPayment, null);
        }

        public boolean hasExistingResponse() {
            return existingResponse != null;
        }
    }
}
