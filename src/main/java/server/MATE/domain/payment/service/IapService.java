package server.MATE.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import server.MATE.domain.payment.dto.response.PaymentOrderStatusResponse;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.policy.IapProductTierCatalog;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.config.TossIapProperties;
import server.MATE.toss.gateway.IapOrderState;
import server.MATE.toss.gateway.IapOrderStatusResult;
import server.MATE.toss.gateway.TossIapGateway;

import java.time.LocalDateTime;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "toss.api", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class IapService {

    private final TossIapGateway tossIapGateway;
    private final IapProductTierCatalog iapProductTierCatalog;
    private final TossAccountRepository tossAccountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCreateService paymentCreateService;
    private final TestDraftRepository testDraftRepository;
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
     * orderId로 기존 Payment를 찾거나, 없으면 티어 카탈로그 + Toss API를 검증해서 새로 저장한다.
     * @return 지급 가능한 Payment, 또는 null (Toss 상태가 결제 완료가 아닌 경우)
     */
    private Payment resolvePayment(String orderId, Long draftId, Long makerId) {
        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
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

        TossIapProperties.Tier tier = iapProductTierCatalog.find(draft.getGoalPpl(), draft.getReward())
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_007));

        TossAccount tossAccount = tossAccountRepository.findByUserId(makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_005));

        IapOrderStatusResult result = tossIapGateway.getOrderStatus(tossAccount.getTossUserKey(), orderId);

        if (tier.sku() == null || !tier.sku().equals(result.sku())) {
            throw new BaseException(BaseErrorCode.PAYMENT_006);
        }

        if (result.status() != IapOrderState.PURCHASED
                && result.status() != IapOrderState.PAYMENT_COMPLETED) {
            return null;
        }

        return savePaymentOrFallback(orderId, draftId, makerId, draft, tier.displayAmount(), result.statusDeterminedAt());
    }

    private Payment savePaymentOrFallback(String orderId, Long draftId, Long makerId,
                                          TestDraft draft, int amount, LocalDateTime approvedAt) {
        try {
            return paymentCreateService.save(Payment.builder()
                    .draftId(draftId)
                    .makerId(makerId)
                    .orderId(orderId)
                    .goalPpl(draft.getGoalPpl())
                    .reward(draft.getReward())
                    .amount(amount)
                    .payMethod(PayMethod.IN_APP_PURCHASE)
                    .payStatus(PayStatus.PAY_SUCCEEDED)
                    .approvedAt(approvedAt)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate grant attempt for orderId={}, falling back to existing record", orderId);
            Payment fallback = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_001));
            if (!fallback.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
            return fallback;
        }
    }

    public PaymentOrderStatusResponse getOrderStatus(String orderId, Long makerId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (!payment.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
        });

        TossAccount tossAccount = tossAccountRepository.findByUserId(makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_005));

        IapOrderStatusResult result = tossIapGateway.getOrderStatus(tossAccount.getTossUserKey(), orderId);

        return new PaymentOrderStatusResponse(
                result.status(),
                result.reason(),
                result.statusDeterminedAt()
        );
    }
}
