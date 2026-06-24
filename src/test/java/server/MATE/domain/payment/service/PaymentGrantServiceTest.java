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
import server.MATE.domain.payment.policy.PaymentAmountCalculator;
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
import server.MATE.toss.client.http.TossHttpClient;
import server.MATE.toss.config.TossProperties;
import server.MATE.toss.dto.request.IapOrderStatusRequest;
import server.MATE.toss.dto.response.IapOrderStatus;
import server.MATE.toss.dto.response.IapOrderStatusResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGrantServiceTest {

    @Mock private TossHttpClient tossHttpClient;
    @Mock private TossAccountRepository tossAccountRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentWriter paymentWriter;
    @Mock private TestDraftRepository testDraftRepository;
    @Mock private PaymentAmountCalculator paymentAmountCalculator;
    @Mock private TestPublishService testPublishService;

    private PaymentGrantService paymentGrantService;

    private static final Long MAKER_ID = 1L;
    private static final Long DRAFT_ID = 10L;
    private static final String ORDER_ID = "order-abc-123";

    @BeforeEach
    void setUp() {
        TossProperties properties = new TossProperties(true, null, null, null, null);
        paymentGrantService = new PaymentGrantService(
                tossHttpClient, properties,
                tossAccountRepository, paymentRepository, paymentWriter,
                testDraftRepository, paymentAmountCalculator, testPublishService
        );
    }

    @Nested
    class GrantTest {

        @Test
        @DisplayName("이미 PAY_SUCCEEDED 상태인 결제가 있으면 재지급하고 true를 반환한다")
        void republishesAndReturnsTrueWhenAlreadySucceeded() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
            verify(testPublishService).publish(100L);
            verify(tossHttpClient, never()).post(any(), any(), any(Consumer.class), any());
        }

        @Test
        @DisplayName("이미 결제 완료됐지만 다른 makerId로 접근하면 COMMON_009 예외를 던진다")
        void throwsCommon009WhenMakerIdMismatchOnExistingPayment() {
            Payment existing = existingPayment(100L, 999L, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_009);
        }

        @Test
        @DisplayName("결제가 PAY_SUCCEEDED가 아닌 상태이면 false를 반환한다")
        void returnsFalseWhenPaymentNotSucceeded() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.REFUND_PENDING);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isFalse();
            verify(testPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("Draft가 없으면 DRAFT_001 예외를 던진다")
        void throwsDraft001WhenDraftNotFound() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.DRAFT_001);
        }

        @Test
        @DisplayName("Draft의 makerId가 다르면 DRAFT_002 예외를 던진다")
        void throwsDraft002WhenDraftMakerIdMismatch() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(999L)));

            assertThatThrownBy(() -> paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.DRAFT_002);
        }

        @Test
        @DisplayName("TossAccount가 없으면 PAYMENT_005 예외를 던진다")
        void throwsPayment005WhenTossAccountNotFound() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_005);
        }

        @Test
        @DisplayName("Toss 상태가 PURCHASED이면 결제를 저장하고 지급 후 true를 반환한다")
        void savesPaymentAndPublishesWhenStatusIsPurchased() {
            TossProperties skuProps = new TossProperties(true, null, null, null,
                    new TossProperties.Iap(List.of()));
            paymentGrantService = new PaymentGrantService(
                    tossHttpClient, skuProps,
                    tossAccountRepository, paymentRepository, paymentWriter,
                    testDraftRepository, paymentAmountCalculator, testPublishService
            );

            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "test_sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(10, 500)).thenReturn(5500);
            Payment saved = savedPayment(200L);
            when(paymentWriter.save(any())).thenReturn(saved);

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
            verify(testPublishService).publish(200L);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentWriter).save(captor.capture());
            Payment captured = captor.getValue();
            assertThat(captured.getPayStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
            assertThat(captured.getPayMethod()).isEqualTo(PayMethod.IN_APP_PURCHASE);
            assertThat(captured.getIsTestPayment()).isFalse();
            assertThat(captured.getAmount()).isEqualTo(5500);
        }

        @Test
        @DisplayName("Toss 상태가 FAILED이면 결제 저장 없이 false를 반환한다")
        void returnsFalseWhenTossStatusIsFailed() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "test_sku", "2025-09-12T16:57:12", IapOrderStatus.FAILED, "user_cancel"));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isFalse();
            verify(paymentWriter, never()).save(any());
            verify(testPublishService, never()).publish(any());
        }

        @Test
        @DisplayName("allowedSkus가 설정됐는데 SKU가 불일치하면 PAYMENT_006 예외를 던진다")
        void throwsPayment006WhenSkuNotAllowed() {
            TossProperties skuProps = new TossProperties(true, null, null, null,
                    new TossProperties.Iap(List.of("allowed_sku")));
            paymentGrantService = new PaymentGrantService(
                    tossHttpClient, skuProps,
                    tossAccountRepository, paymentRepository, paymentWriter,
                    testDraftRepository, paymentAmountCalculator, testPublishService
            );

            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "unknown_sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));

            assertThatThrownBy(() -> paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_006);
        }

        @Test
        @DisplayName("allowedSkus가 비어있으면 SKU 검증을 건너뛴다")
        void skipsSkuValidationWhenAllowedSkusEmpty() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "any_sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(any(Integer.class), any(Integer.class))).thenReturn(5500);
            when(paymentWriter.save(any())).thenReturn(savedPayment(200L));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("동시 요청으로 DataIntegrityViolationException 발생 시 기존 결제로 fallback해 지급한다")
        void fallsBackToExistingPaymentOnDuplicateKeyConflict() {
            Payment fallback = existingPayment(300L, MAKER_ID, PayStatus.PAY_SUCCEEDED);

            when(paymentRepository.findByOrderNo(ORDER_ID))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(fallback));
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(any(Integer.class), any(Integer.class))).thenReturn(5500);
            when(paymentWriter.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
            verify(testPublishService).publish(300L);
        }

        @Test
        @DisplayName("statusDeterminedAt이 null이어도 NPE 없이 현재 시각으로 저장한다")
        void handlesNullStatusDeterminedAtWithoutNpe() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, null, null, IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(any(Integer.class), any(Integer.class))).thenReturn(5500);
            when(paymentWriter.save(any())).thenReturn(savedPayment(200L));

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentWriter).save(captor.capture());
            assertThat(captor.getValue().getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("publish가 실패해도 grant는 true를 반환한다")
        void returnsTrueEvenWhenPublishFails() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, null, "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(any(Integer.class), any(Integer.class))).thenReturn(5500);
            Payment saved = savedPayment(200L);
            when(paymentWriter.save(any())).thenReturn(saved);
            doThrow(new RuntimeException("publish failed")).when(testPublishService).publish(200L);

            boolean result = paymentGrantService.grant(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
            verify(paymentWriter).save(any());
        }
    }

    @Nested
    class RestoreTest {

        @Test
        @DisplayName("기존 Payment가 있으면 orderId만으로 publish를 재시도한다")
        void publishesWithOrderIdOnlyWhenPaymentExists() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));

            boolean result = paymentGrantService.restore(ORDER_ID, null, MAKER_ID);

            assertThat(result).isTrue();
            verify(testPublishService).publish(100L);
            verify(tossHttpClient, never()).post(any(), any(), any(Consumer.class), any());
        }

        @Test
        @DisplayName("Payment가 없고 draftId가 null이면 PAYMENT_001 예외를 던진다")
        void throwsPayment001WhenNoPaymentAndNoDraftId() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentGrantService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_001);
        }

        @Test
        @DisplayName("Payment가 없지만 draftId가 있으면 Toss 검증 후 저장하고 publish한다")
        void verifiesAndPublishesWhenNoPaymentButDraftIdProvided() {
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.empty());
            when(testDraftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftOf(MAKER_ID)));
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, null, "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));
            when(paymentAmountCalculator.totalAmount(any(Integer.class), any(Integer.class))).thenReturn(5500);
            Payment saved = savedPayment(200L);
            when(paymentWriter.save(any())).thenReturn(saved);

            boolean result = paymentGrantService.restore(ORDER_ID, DRAFT_ID, MAKER_ID);

            assertThat(result).isTrue();
            verify(paymentWriter).save(any());
            verify(testPublishService).publish(200L);
        }

        @Test
        @DisplayName("restore에서 publish가 실패하면 예외를 전파한다")
        void propagatesExceptionWhenPublishFailsOnRestore() {
            Payment existing = existingPayment(100L, MAKER_ID, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("publish failed")).when(testPublishService).publish(100L);

            assertThatThrownBy(() -> paymentGrantService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("publish failed");
        }

        @Test
        @DisplayName("다른 makerId로 복원을 시도하면 COMMON_009 예외를 던진다")
        void throwsCommon009WhenMakerIdMismatchOnRestore() {
            Payment existing = existingPayment(100L, 999L, PayStatus.PAY_SUCCEEDED);
            when(paymentRepository.findByOrderNo(ORDER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> paymentGrantService.restore(ORDER_ID, null, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.COMMON_009);
        }
    }

    @Nested
    class GetOrderStatusTest {

        @Test
        @DisplayName("TossAccount가 없으면 PAYMENT_005 예외를 던진다")
        void throwsPayment005WhenTossAccountNotFound() {
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentGrantService.getOrderStatus(ORDER_ID, MAKER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.PAYMENT_005);
        }

        @Test
        @DisplayName("Toss 응답을 그대로 PaymentOrderStatusResponse로 반환한다")
        void returnsOrderStatusFromTossResponse() {
            when(tossAccountRepository.findByUserId(MAKER_ID)).thenReturn(Optional.of(tossAccount()));
            when(tossHttpClient.post(any(), any(), any(Consumer.class), eq(IapOrderStatusResponse.class)))
                    .thenReturn(new IapOrderStatusResponse(ORDER_ID, "test_sku", "2025-09-12T16:57:12", IapOrderStatus.PURCHASED, null));

            PaymentOrderStatusResponse response = paymentGrantService.getOrderStatus(ORDER_ID, MAKER_ID);

            assertThat(response.status()).isEqualTo(IapOrderStatus.PURCHASED);
            assertThat(response.statusDeterminedAt()).isEqualTo("2025-09-12T16:57:12");

            ArgumentCaptor<IapOrderStatusRequest> captor = ArgumentCaptor.forClass(IapOrderStatusRequest.class);
            verify(tossHttpClient).post(any(), captor.capture(), any(Consumer.class), eq(IapOrderStatusResponse.class));
            assertThat(captor.getValue().orderId()).isEqualTo(ORDER_ID);
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
                .orderNo(ORDER_ID)
                .goalPpl(10)
                .reward(500)
                .amount(5500)
                .paidAmount(5500)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .transactionId(ORDER_ID)
                .payStatus(status)
                .isTestPayment(false)
                .approvedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }

    private Payment savedPayment(Long id) {
        Payment payment = Payment.builder()
                .draftId(DRAFT_ID).makerId(MAKER_ID).orderNo(ORDER_ID)
                .goalPpl(10).reward(500).amount(5500).paidAmount(5500)
                .payMethod(PayMethod.IN_APP_PURCHASE).transactionId(ORDER_ID)
                .payStatus(PayStatus.PAY_SUCCEEDED).isTestPayment(false)
                .approvedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
