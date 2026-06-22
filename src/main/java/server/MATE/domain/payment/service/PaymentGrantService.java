package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.dto.response.PaymentOrderStatusResponse;
import server.MATE.domain.payment.policy.PaymentAmountCalculator;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.dto.request.IapOrderStatusRequest;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentGrantService {

    private static final String IAP_ORDER_STATUS_PATH = "/api-partner/v1/apps-in-toss/order/get-order-status";

    private final TossHttpClient tossHttpClient;
    private final TossAccountRepository tossAccountRepository;
    private final PaymentRepository paymentRepository;
    private final TestDraftRepository testDraftRepository;
    private final PaymentAmountCalculator paymentAmountCalculator;
    private final TestPublishService testPublishService;

    @Transactional
    public boolean grant(String orderId, Long draftId, Long makerId) {
        Payment existing = paymentRepository.findByOrderNo(orderId).orElse(null);
        if (existing != null) {
            if (!existing.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
            if (existing.getPayStatus() == PayStatus.PAY_SUCCEEDED) {
                testPublishService.publish(existing.getId());
                return true;
            }
            return false;
        }

        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        if (!draft.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.DRAFT_002);
        }
        draft.validateAmountFields();

        TossAccount tossAccount = tossAccountRepository.findByUserId(makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_005));

        IapOrderStatusResponse statusResponse = tossHttpClient.post(
                IAP_ORDER_STATUS_PATH,
                new IapOrderStatusRequest(orderId),
                headers -> headers.set("x-toss-user-key", String.valueOf(tossAccount.getTossUserKey())),
                IapOrderStatusResponse.class
        );

        if (statusResponse.status() != IapOrderStatus.PURCHASED
                && statusResponse.status() != IapOrderStatus.PAYMENT_COMPLETED) {
            return false;
        }

        int amount = paymentAmountCalculator.totalAmount(draft.getGoalPpl(), draft.getReward());
        LocalDateTime approvedAt = parseApprovedAt(statusResponse.statusDeterminedAt());

        Payment payment = paymentRepository.save(Payment.builder()
                .draftId(draftId)
                .makerId(makerId)
                .orderNo(orderId)
                .goalPpl(draft.getGoalPpl())
                .reward(draft.getReward())
                .amount(amount)
                .paidAmount(amount)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .transactionId(orderId)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .isTestPayment(false)
                .approvedAt(approvedAt)
                .build());

        testPublishService.publish(payment.getId());
        return true;
    }

    private LocalDateTime parseApprovedAt(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value);
        }
    }

    public PaymentOrderStatusResponse getOrderStatus(String orderId, Long makerId) {
        TossAccount tossAccount = tossAccountRepository.findByUserId(makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_005));

        IapOrderStatusResponse statusResponse = tossHttpClient.post(
                IAP_ORDER_STATUS_PATH,
                new IapOrderStatusRequest(orderId),
                headers -> headers.set("x-toss-user-key", String.valueOf(tossAccount.getTossUserKey())),
                IapOrderStatusResponse.class
        );

        return new PaymentOrderStatusResponse(
                statusResponse.status(),
                statusResponse.reason(),
                statusResponse.statusDeterminedAt()
        );
    }
}
