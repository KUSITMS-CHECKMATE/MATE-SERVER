package server.MATE.domain.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.dto.response.PaymentOrderStatusResponse;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.policy.IapProductTierCatalog;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.users.entity.Role;
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.config.TossIapProperties;
import server.MATE.toss.gateway.IapOrderState;
import server.MATE.toss.gateway.IapOrderStatusResult;
import server.MATE.toss.gateway.TossIapGateway;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IapServiceTest {

    @Mock private TossIapGateway tossIapGateway;
    @Mock private IapProductTierCatalog iapProductTierCatalog;
    @Mock private TossAccountRepository tossAccountRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentCreateService paymentCreateService;
    @Mock private TestDraftRepository testDraftRepository;
    @Mock private TestPublishService testPublishService;

    private IapService iapService;

    private static final Long MAKER_ID = 1L;
    private static final Long DRAFT_ID = 10L;
    private static final String ORDER_ID = "order-abc-123";
    private static final LocalDateTime APPROVED_AT = LocalDateTime.parse("2025-09-12T16:57:12");
    private static final TossIapProperties.Tier TIER =
            new TossIapProperties.Tier(10, 500, "sku_10_500", 5500);

    @BeforeEach
    void setUp() {
        iapService = new IapService(
                tossIapGateway, iapProductTierCatalog,
                tossAccountRepository, paymentRepository, paymentCreateService,
                testDraftRepository, testPublishService
        );
    }

    @Nested
    class GrantTest {

        @Test
        @DisplayName("이미 PAY_SUCCEEDED 상태인 결제가 있으면 재지급하고 true를 반환한다")
        void republishesAndReturnsTrueWhenAlreadySucceeded() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            verify(testPublishService).publish(100L);
            verify(tossIapGateway, never()).getOrderStatus(any(), any());
        }

        @Test
        @DisplayName("이미 결제 완료됐지만 다른 makerId로 접근하면 COMMON_009 예외를 던진다")
        void throwsCommon009WhenMakerIdMismatchOnExistingPayment() {
            Payment existing = existingPayment(100L, 999L, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_009);
        }

        @Test
        @DisplayName("기존 결제가 PAY_SUCCEEDED가 아닌 상태이면 PAYMENT_003 예외를 던진다")
        void throwsPayment003WhenExistingPaymentNotSucceeded() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.REFUND_PENDING);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_003);
            verify(testPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("Draft가 없으면 DRAFT_001 예외를 던진다")
        void throwsDraft001WhenDraftNotFound() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.DRAFT_001);
        }

        @Test
        @DisplayName("Draft의 makerId가 다르면 DRAFT_002 예외를 던진다")
        void throwsDraft002WhenDraftMakerIdMismatch() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(999L)));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.DRAFT_002);
        }

        @Test
        @DisplayName("goalPpl/reward 조합이 티어 카탈로그에 없으면 DRAFT_007 예외를 던진다")
        void throwsDraft007WhenTierNotFound() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.DRAFT_007);

            verify(tossAccountRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("TossAccount가 없으면 PAYMENT_005 예외를 던진다")
        void throwsPayment005WhenTossAccountNotFound() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_005);
        }

        @Test
        @DisplayName("Toss 게이트웨이 호출 중 예외가 발생하면 TOSS_SERVER_VERIFICATION_FAILED를 던진다")
        void throwsTossServerVerificationFailedWhenGatewayThrows() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenThrow(new RuntimeException("Toss server error"));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.TOSS_SERVER_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("Toss가 반환한 sku가 이 draft에 기대되는 티어의 sku와 다르면 PAYMENT_006 예외를 던진다")
        void throwsPayment006WhenSkuDoesNotMatchExpectedTier() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "unexpected_sku", null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_006);
        }

        @Test
        @DisplayName("티어에 등록된 sku가 null이면 Toss 응답의 sku가 null이어도 PAYMENT_006 예외를 던진다")
        void throwsPayment006WhenTierSkuIsNullEvenIfResultSkuIsAlsoNull() {
            TossIapProperties.Tier tierWithNullSku = new TossIapProperties.Tier(10, 500, null, 5500);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(tierWithNullSku));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, null, null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_006);

            verify(paymentCreateService, never()).save(any());
        }

        @Test
        @DisplayName("Toss 상태가 FAILED이면 APP_MARKET_VERIFICATION_FAILED 예외를 던진다")
        void throwsAppMarketVerificationFailedWhenStatusIsFailed() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.FAILED, null, "user_cancel", APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.APP_MARKET_VERIFICATION_FAILED);

            verify(paymentCreateService, never()).save(any());
            verify(testPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("Toss 상태가 ERROR이면 APP_MARKET_VERIFICATION_FAILED 예외를 던진다")
        void throwsAppMarketVerificationFailedWhenStatusIsError() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.ERROR, null, null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.APP_MARKET_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("Toss 상태가 MINIAPP_MISMATCH이면 APP_MARKET_VERIFICATION_FAILED 예외를 던진다")
        void throwsAppMarketVerificationFailedWhenStatusIsMiniappMismatch() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.MINIAPP_MISMATCH, null, null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.APP_MARKET_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("Toss 상태가 ORDER_IN_PROGRESS이면 ORDER_IN_PROGRESS(409) 예외를 던진다")
        void throwsOrderInProgressWhenStatusIsOrderInProgress() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.ORDER_IN_PROGRESS, null, null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.ORDER_IN_PROGRESS);
            verify(paymentCreateService, never()).save(any());
        }

        @Test
        @DisplayName("Toss 상태가 NOT_FOUND이면 ORDER_NOT_FOUND(404) 예외를 던진다")
        void throwsOrderNotFoundWhenStatusIsNotFound() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.NOT_FOUND, null, null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("Toss 상태가 REFUNDED이면 ORDER_ALREADY_REFUNDED 예외를 던진다")
        void throwsOrderAlreadyRefundedWhenStatusIsRefunded() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.REFUNDED, "sku_10_500", null, APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.ORDER_ALREADY_REFUNDED);
        }

        @Test
        @DisplayName("결제 미완료 응답은 sku가 비어 있어도 SKU 오류가 아니라 상태 기반 에러를 던진다")
        void reportsStateErrorNotSkuErrorWhenOrderIncompleteWithNullSku() {
            // 회귀 방지: Toss가 미완료 주문에 sku를 안 내려주는데 SKU 대조를 먼저 하면
            // 실제 원인(결제 진행 중)이 "허용되지 않은 SKU"로 둔갑한다.
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.ORDER_IN_PROGRESS, null, "결제 진행 중", APPROVED_AT));

            assertThatThrownBy(() -> iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isNotEqualTo(BaseErrorCode.PAYMENT_006);
        }

        @Test
        @DisplayName("Toss 상태가 PURCHASED이면 결제를 티어 등록가로 저장하고 지급한다")
        void savesPaymentAndPublishesWhenStatusIsPurchased() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "sku_10_500", null, APPROVED_AT));
            Payment saved = savedPayment(200L);
            when(paymentCreateService.save(any())).thenReturn(saved);

            iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            verify(testPublishService).publish(200L);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCreateService).save(captor.capture());
            Payment captured = captor.getValue();
            assertThat(captured.getPayStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
            assertThat(captured.getPayMethod()).isEqualTo(PayMethod.IN_APP_PURCHASE);
            assertThat(captured.getAmount()).isEqualTo(5500);
            assertThat(captured.getApprovedAt()).isEqualTo(APPROVED_AT);
        }

        @Test
        @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 기존 결제로 fallback해 지급한다")
        void fallsBackToExistingPaymentOnDuplicateKeyConflict() {
            Payment fallback = existingPayment(300L, MAKER_ID, PayStatus.PAY_SUCCEEDED);

            when(paymentRepository.findByOrderId(ORDER_ID))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(fallback));
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "sku_10_500", null, APPROVED_AT));
            when(paymentCreateService.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

            iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            verify(testPublishService).publish(300L);
        }

        @Test
        @DisplayName("publish가 실패해도 grant는 true를 반환한다")
        void returnsTrueEvenWhenPublishFails() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "sku_10_500", null, APPROVED_AT));
            Payment saved = savedPayment(200L);
            when(paymentCreateService.save(any())).thenReturn(saved);
            doThrow(new RuntimeException("publish failed")).when(testPublishService).publish(200L);

            iapService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            verify(paymentCreateService).save(any());
        }

    }

    @Nested
    class RestoreTest {

        @Test
        @DisplayName("기존 Payment가 있으면 orderId만으로 publish를 재시도하고 retryCount를 증가시킨다")
        void publishesWithOrderIdOnlyWhenPaymentExists() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            iapService.restore(ORDER_ID, null, MAKER_ID);

            assertThat(existing.getRetryCount()).isEqualTo(1);
            verify(paymentRepository).save(existing);
            verify(testPublishService).publish(100L);
            verify(tossIapGateway, never()).getOrderStatus(any(), any());
        }

        @Test
        @DisplayName("Payment가 없고 draftId가 null이면 PAYMENT_001 예외를 던진다")
        void throwsPayment001WhenNoPaymentAndNoDraftId() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iapService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_001);
        }

        @Test
        @DisplayName("Payment가 없지만 draftId가 있으면 Toss 검증 후 저장하고 publish한다")
        void verifiesAndPublishesWhenNoPaymentButDraftIdProvided() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(iapProductTierCatalog.find(10, 500)).thenReturn(Optional.of(TIER));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "sku_10_500", null, APPROVED_AT));
            Payment saved = savedPayment(200L);
            when(paymentCreateService.save(any())).thenReturn(saved);

            iapService.restore(ORDER_ID, DRAFT_ID, MAKER_ID);

            verify(paymentCreateService).save(any());
            verify(testPublishService).publish(200L);
        }

        @Test
        @DisplayName("restore에서 publish가 실패하면 PRODUCT_NOT_GRANTED_BY_PARTNER 예외를 던진다")
        void throwsProductNotGrantedByPartnerWhenPublishFailsOnRestore() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("publish failed")).when(testPublishService).publish(100L);

            assertThatThrownBy(() -> iapService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PRODUCT_NOT_GRANTED_BY_PARTNER);
        }

        @Test
        @DisplayName("다른 makerId로 복원을 시도하면 COMMON_009 예외를 던진다")
        void throwsCommon009WhenMakerIdMismatchOnRestore() {
            Payment existing = existingPayment(100L, 999L, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> iapService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_009);
        }

        @Test
        @DisplayName("retryCount가 4 이상이면 RETRY_LIMIT_EXCEEDED 예외를 던진다")
        void throwsRetryLimitExceededWhenRetryCountAtLimit() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            ReflectionTestUtils.setField(existing, "retryCount", 4);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> iapService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.RETRY_LIMIT_EXCEEDED);

            verify(testPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("retryCount가 3이면 4번째 시도를 허용한다")
        void allowsFourthRestoreAttemptWhenRetryCountIsThree() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            ReflectionTestUtils.setField(existing, "retryCount", 3);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(existing));

            iapService.restore(ORDER_ID, null, MAKER_ID);

            assertThat(existing.getRetryCount()).isEqualTo(4);
            verify(testPublishService).publish(100L);
        }
    }

    @Nested
    class GetOrderStatusTest {

        @Test
        @DisplayName("TossAccount가 없으면 PAYMENT_005 예외를 던진다")
        void throwsPayment005WhenTossAccountNotFound() {
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> iapService.getOrderStatus(ORDER_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_005);
        }

        @Test
        @DisplayName("게이트웨이 결과를 그대로 PaymentOrderStatusResponse로 반환한다")
        void returnsOrderStatusFromGatewayResult() {
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossIapGateway.getOrderStatus(eq(777L), eq(ORDER_ID)))
                    .thenReturn(new IapOrderStatusResult(IapOrderState.PURCHASED, "sku_10_500", null, APPROVED_AT));

            PaymentOrderStatusResponse response = iapService.getOrderStatus(ORDER_ID, MAKER_ID);

            assertThat(response.status()).isEqualTo(IapOrderState.PURCHASED);
            assertThat(response.statusDeterminedAt()).isEqualTo(APPROVED_AT);
            verify(tossIapGateway).getOrderStatus(777L, ORDER_ID);
        }
    }

    // --- fixtures ---

    private TestDraft draftOf(Long makerId) {
        TestDraft draft = TestDraft.builder()
                .makerId(makerId)
                .goalPpl(10)
                .reward(500)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59))
                .status(TestDraftStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(draft, "id", DRAFT_ID);
        return draft;
    }

    private TossAccount tossAccount() {
        Users user = Users.builder().ci("ci").name("tester").role(Role.USER).build();
        ReflectionTestUtils.setField(user, "id", MAKER_ID);
        return TossAccount.builder()
                .user(user)
                .tossUserKey(777L)
                .encryptedTossRefreshToken("enc")
                .scope("user_ci")
                .isLinked(true)
                .lastLoginAt(LocalDateTime.now())
                .lastTokenRefreshedAt(LocalDateTime.now())
                .build();
    }

    private Payment existingPayment(Long id, Long makerId, PayStatus status) {
        Payment payment = Payment.builder()
                .draftId(DRAFT_ID)
                .makerId(makerId)
                .orderId(ORDER_ID)
                .goalPpl(10)
                .reward(500)
                .amount(5500)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .payStatus(status)
                .approvedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    private Payment savedPayment(Long id) {
        Payment payment = Payment.builder()
                .draftId(DRAFT_ID).makerId(MAKER_ID).orderId(ORDER_ID)
                .goalPpl(10).reward(500).amount(5500)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .approvedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
