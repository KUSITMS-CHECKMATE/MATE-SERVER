package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
import server.MATE.toss.config.TossProperties;
import server.MATE.toss.dto.request.IapOrderStatusRequest;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PaymentGrantService {

    private static final String IAP_ORDER_STATUS_PATH = "/api-partner/v1/apps-in-toss/order/get-order-status";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TossHttpClient tossHttpClient;
    private final TossProperties tossProperties;
    private final TossAccountRepository tossAccountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWriter paymentWriter;
    private final TestDraftRepository testDraftRepository;
    private final PaymentAmountCalculator paymentAmountCalculator;
    private final TestPublishService testPublishService;

    /**
     * processProductGrant 콜백에서 호출.
     * Payment 저장 후 publish를 best-effort로 시도하되,
     * publish 실패해도 true를 반환해서 30초 타임아웃을 지킨다.
     * publish가 안 된 건은 restore로 재시도.
     */
    public boolean grant(String orderId, Long draftId, Long makerId) {
        Payment payment = resolvePayment(orderId, draftId, makerId);
        if (payment == null) {
            return false;
        }

        try {
            testPublishService.publish(payment.getId());
        } catch (Exception e) {
            log.warn("Publish failed after grant for paymentId={}, will be retried via restore",
                    payment.getId(), e);
        }
        return true;
    }

    /**
     * getPendingOrders 복원 플로우에서 호출.
     * Payment가 이미 있으면 orderId만으로 publish를 재시도한다.
     * Payment가 없으면 draftId를 사용해 결제 검증부터 수행한다.
     * publish 실패 시 예외를 전파해서 클라이언트가 재시도를 판단한다.
     */
    public boolean restore(String orderId, Long draftId, Long makerId) {
        Payment payment = resolvePayment(orderId, draftId, makerId);
        if (payment == null) {
            return false;
        }

        testPublishService.publish(payment.getId());
        return true;
    }

    /**
     * orderId로 기존 Payment를 찾거나, 없으면 Toss API를 검증해서 새로 저장한다.
     * @return 지급 가능한 Payment, 또는 null (Toss 상태가 결제 완료가 아닌 경우)
     */
    private Payment resolvePayment(String orderId, Long draftId, Long makerId) {
        Payment existing = paymentRepository.findByOrderNo(orderId).orElse(null);
        if (existing != null) {
            if (!existing.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
            if (existing.getPayStatus() == PayStatus.PAY_SUCCEEDED) {
                return existing;
            }
            return null;
        }

        if (draftId == null) {
            throw new BaseException(BaseErrorCode.PAYMENT_001);
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

        validateSku(statusResponse.sku());

        if (statusResponse.status() != IapOrderStatus.PURCHASED
                && statusResponse.status() != IapOrderStatus.PAYMENT_COMPLETED) {
            return null;
        }

        int amount = paymentAmountCalculator.totalAmount(draft.getGoalPpl(), draft.getReward());
        LocalDateTime approvedAt = parseApprovedAt(statusResponse.statusDeterminedAt());

        return savePaymentOrFallback(orderId, draftId, makerId, draft, amount, approvedAt);
    }

    private Payment savePaymentOrFallback(String orderId, Long draftId, Long makerId,
                                          TestDraft draft, int amount, LocalDateTime approvedAt) {
        try {
            return paymentWriter.save(Payment.builder()
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
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate grant attempt for orderId={}, falling back to existing record", orderId);
            Payment fallback = paymentRepository.findByOrderNo(orderId)
                    .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
            if (!fallback.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
            return fallback;
        }
    }

    private void validateSku(String sku) {
        var allowedSkus = tossProperties.iap().allowedSkus();
        if (allowedSkus.isEmpty()) {
            return;
        }
        if (sku == null || !allowedSkus.contains(sku)) {
            throw new BaseException(BaseErrorCode.PAYMENT_006);
        }
    }

    private LocalDateTime parseApprovedAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(KST);
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(value).atZoneSameInstant(KST).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                log.warn("Unrecognized approvedAt format='{}', falling back to now", value);
                return LocalDateTime.now(KST);
            }
        }
    }

    public PaymentOrderStatusResponse getOrderStatus(String orderId, Long makerId) {
        paymentRepository.findByOrderNo(orderId).ifPresent(payment -> {
            if (!payment.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
        });

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
