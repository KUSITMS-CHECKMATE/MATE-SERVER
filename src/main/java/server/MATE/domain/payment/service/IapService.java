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

import server.MATE.domain.payment.util.OrderNoGenerator;

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

    // 결제 검증 후 테스트 publish 수행
    public void grant(String orderId, Long draftId, Long makerId) {
        log.info("grant 진입: orderId={}, draftId={}, makerId={}", orderId, draftId, makerId);
        Payment payment = resolvePayment(orderId, draftId, makerId);

        try {
            testPublishService.publish(payment.getId());
        } catch (Exception e) {
            log.warn("Publish failed after grant for paymentId={}, will be retried via restore",
                    payment.getId(), e);
        }
    }

    // 결제 후 테스트 publish 실패를 복원
    public void restore(String orderId, Long draftId, Long makerId) {
        log.info("restore 진입: orderId={}, draftId={}, makerId={}", orderId, draftId, makerId);
        Payment payment = resolvePayment(orderId, draftId, makerId);
        if (payment.getRetryCount() >= 4) {
            throw new BaseException(BaseErrorCode.RETRY_LIMIT_EXCEEDED);
        }
        payment.incrementRetryCount();
        paymentRepository.save(payment);
        try {
            testPublishService.publish(payment.getId());
        } catch (Exception e) {
            log.warn("Publish failed during restore for paymentId={}", payment.getId(), e);
            throw new BaseException(BaseErrorCode.PRODUCT_NOT_GRANTED_BY_PARTNER);
        }
    }

    // 기존 Payment를 찾거나 없으면 Toss 주문 검증 후 새로 저장
    private Payment resolvePayment(String orderId, Long draftId, Long makerId) {
        Payment existing = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            if (!existing.getMakerId().equals(makerId)) {
                throw new BaseException(BaseErrorCode.COMMON_009);
            }
            if (existing.getPayStatus() == PayStatus.PAY_SUCCEEDED) {
                return existing;
            }
            log.warn("Grant rejected: payment for orderId={} is in status={}", orderId, existing.getPayStatus());
            throw new BaseException(BaseErrorCode.PAYMENT_003);
        }

        if (draftId == null) {
            throw new BaseException(BaseErrorCode.PAYMENT_001);
        }

        TestDraft draft = testDraftRepository.findById(draftId).orElse(null);
        if (draft == null) {
            return resolveAlreadyPublishedPayment(draftId, makerId);
        }
        if (!draft.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.DRAFT_002);
        }
        draft.validateAmountFields();

        TossIapProperties.Tier tier = iapProductTierCatalog.find(draft.getGoalPpl(), draft.getReward())
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_007));

        TossAccount tossAccount = tossAccountRepository.findByUserId(makerId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.PAYMENT_005));

        IapOrderStatusResult result;
        try {
            result = tossIapGateway.getOrderStatus(tossAccount.getTossUserKey(), orderId);
        } catch (Exception e) {
            // TODO: Toss IAP가 은행 점검 에러를 별도 에러 코드로 내려줄 경우 BANK_MAINTENANCE로 분기 (Toss IAP API 문서 확인 필요)
            log.warn("Toss IAP gateway error for orderId={}", orderId, e);
            throw new BaseException(BaseErrorCode.TOSS_SERVER_VERIFICATION_FAILED);
        }

        verifyOrderState(orderId, result);

        if (tier.sku() == null || !tier.sku().equals(result.sku())) {
            log.warn("Grant rejected: SKU mismatch for orderId={}, expected={}, actual={}",
                    orderId, tier.sku(), result.sku());
            throw new BaseException(BaseErrorCode.PAYMENT_006);
        }

        return savePaymentOrFallback(orderId, draftId, makerId, draft, tier.displayAmount(), result.statusDeterminedAt());
    }

    // TestPublishService가 발행 성공 시 draft를 삭제하므로, 같은 draftId로 재시도가 들어오면
    // draft가 없는 게 아니라 이미 다른 orderId로 성공 처리된 것일 수 있다. 그 경우 기존 결제를 반환한다.
    private Payment resolveAlreadyPublishedPayment(Long draftId, Long makerId) {
        Payment payment = paymentRepository
                .findFirstByDraftIdAndPayStatusOrderByCreatedAtDesc(draftId, PayStatus.PAY_SUCCEEDED)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        if (!payment.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.COMMON_009);
        }
        return payment;
    }

    // 지급 가능 상태 (PURCHASED, PAYMENT_COMPLETED)가 아니면 상태별 에러 코드 반환
    private void verifyOrderState(String orderId, IapOrderStatusResult result) {
        if (result.status() == IapOrderState.PURCHASED
                || result.status() == IapOrderState.PAYMENT_COMPLETED) {
            return;
        }

        log.warn("Grant rejected: Toss order status={} for orderId={}, reason={}",
                result.status(), orderId, result.reason());

        throw switch (result.status()) {
            case ORDER_IN_PROGRESS -> new BaseException(BaseErrorCode.ORDER_IN_PROGRESS);
            case NOT_FOUND -> new BaseException(BaseErrorCode.ORDER_NOT_FOUND);
            case REFUNDED -> new BaseException(BaseErrorCode.ORDER_ALREADY_REFUNDED);
            case FAILED, ERROR, MINIAPP_MISMATCH ->
                    new BaseException(BaseErrorCode.APP_MARKET_VERIFICATION_FAILED);
            default -> new BaseException(BaseErrorCode.PAYMENT_003);
        };
    }

    private Payment savePaymentOrFallback(String orderId, Long draftId, Long makerId,
                                          TestDraft draft, int amount, LocalDateTime approvedAt) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return paymentCreateService.save(Payment.builder()
                        .draftId(draftId)
                        .makerId(makerId)
                        .orderId(orderId)
                        .orderNo(OrderNoGenerator.generate())
                        .goalPpl(draft.getGoalPpl())
                        .reward(draft.getReward())
                        .amount(amount)
                        .payMethod(PayMethod.IN_APP_PURCHASE)
                        .payStatus(PayStatus.PAY_SUCCEEDED)
                        .approvedAt(approvedAt)
                        .build());
            } catch (DataIntegrityViolationException e) {
                // order_id 중복이면 기존 레코드로 fallback
                Payment fallback = paymentRepository.findByOrderId(orderId).orElse(null);
                if (fallback != null) {
                    log.warn("Duplicate grant attempt for orderId={}, falling back to existing record", orderId);
                    if (!fallback.getMakerId().equals(makerId)) {
                        throw new BaseException(BaseErrorCode.COMMON_009);
                    }
                    return fallback;
                }
                // order_no 충돌이면 새 orderNo로 재시도
                log.warn("orderNo collision on attempt {}, retrying", attempt + 1);
            }
        }
        throw new BaseException(BaseErrorCode.COMMON_999);
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
