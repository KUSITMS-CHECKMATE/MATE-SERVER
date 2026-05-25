package server.MATE.domain.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.dto.response.PaymentCreateResponse;
import server.MATE.domain.payment.dto.response.PaymentExecuteResponse;
import server.MATE.domain.payment.dto.response.PaymentRefundResponse;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.test.service.TestPublishService;
import server.MATE.domain.testdraft.service.TestDraftPublishStateService;
import server.MATE.toss.dto.request.TossPaymentCreateRequest;
import server.MATE.toss.dto.request.TossPaymentExecuteRequest;
import server.MATE.toss.dto.request.TossPaymentRefundRequest;
import server.MATE.toss.dto.response.TossPaymentCreateResponse;
import server.MATE.toss.dto.response.TossPaymentExecuteResponse;
import server.MATE.toss.dto.response.TossPaymentRefundResponse;
import server.MATE.toss.gateway.TossPaymentGateway;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentGateway mockPaymentGateway;

    @Mock
    private PaymentPrepareService paymentPrepareService;

    @Mock
    private PaymentCreateStateService paymentCreateStateService;

    @Mock
    private PaymentExecuteStateService paymentExecuteStateService;

    @Mock
    private PaymentRefundStateService paymentRefundStateService;

    @Mock
    private TestPublishService testPublishService;

    @Mock
    private TestDraftPublishStateService testDraftPublishStateService;

    private MockPaymentService mockPaymentService;

    @BeforeEach
    void setUp() {
        mockPaymentService = new MockPaymentService(
                paymentRepository,
                mockPaymentGateway,
                paymentPrepareService,
                paymentCreateStateService,
                paymentExecuteStateService,
                paymentRefundStateService,
                testPublishService,
                testDraftPublishStateService
        );
    }

    @Test
    @DisplayName("결제 생성 시 amount 계산, payment 생성, payToken 저장을 수행한다")
    void createsPayment() {
        PaymentPrepareService.PaymentPreparation preparation =
                PaymentPrepareService.PaymentPreparation.forGatewayCall(
                        20L, 10L, "order-no", 30000, true
                );

        given(paymentPrepareService.prepare(10L, 1L, true)).willReturn(preparation);
        given(mockPaymentGateway.createPayment(any(TossPaymentCreateRequest.class)))
                .willReturn(new TossPaymentCreateResponse("mock-pay-token"));

        PaymentCreateResponse response = mockPaymentService.createPayment(10L, 1L, true);

        assertThat(response.paymentId()).isEqualTo(20L);
        assertThat(response.amount()).isEqualTo(30000);
        assertThat(response.payToken()).isEqualTo("mock-pay-token");
    }

    @Test
    @DisplayName("이미 PAY_CREATED 상태인 결제가 있으면 기존 결제를 재사용한다")
    void reusesCreatedPayment() {
        PaymentCreateResponse existing = new PaymentCreateResponse(
                20L, 10L, "order-no", 30000, "mock-pay-token", true
        );
        PaymentPrepareService.PaymentPreparation preparation =
                PaymentPrepareService.PaymentPreparation.withExistingResponse(existing);

        given(paymentPrepareService.prepare(10L, 1L, true)).willReturn(preparation);

        PaymentCreateResponse response = mockPaymentService.createPayment(10L, 1L, true);

        assertThat(response.paymentId()).isEqualTo(20L);
        assertThat(response.orderNo()).isEqualTo("order-no");
        assertThat(response.payToken()).isEqualTo("mock-pay-token");
    }

    @Test
    @DisplayName("이미 PAY_SUCCEEDED 상태인 결제가 있으면 새 결제 생성을 막는다")
    void blocksSucceededPaymentRecreation() {
        given(paymentPrepareService.prepare(10L, 1L, true))
                .willThrow(new server.MATE.global.common.exception.BaseException(server.MATE.global.common.exception.BaseErrorCode.PAYMENT_002));

        assertThatThrownBy(() -> mockPaymentService.createPayment(10L, 1L, true))
                .isInstanceOf(server.MATE.global.common.exception.BaseException.class)
                .hasMessageContaining("결제를 생성할 수 없는 상태입니다.");
    }

    @Test
    @DisplayName("PAY_FAILED 상태인 결제가 있으면 같은 row를 재사용해 재생성한다")
    void recreatesFailedPayment() {
        PaymentPrepareService.PaymentPreparation preparation =
                PaymentPrepareService.PaymentPreparation.forGatewayCall(
                        20L, 10L, "new-order", 30000, true
                );
        given(paymentPrepareService.prepare(10L, 1L, true)).willReturn(preparation);
        given(mockPaymentGateway.createPayment(any(TossPaymentCreateRequest.class)))
                .willReturn(new TossPaymentCreateResponse("new-mock-pay-token"));

        PaymentCreateResponse response = mockPaymentService.createPayment(10L, 1L, true);

        assertThat(response.paymentId()).isEqualTo(20L);
        assertThat(response.payToken()).isEqualTo("new-mock-pay-token");
        assertThat(response.orderNo()).isEqualTo("new-order");
    }

    @Test
    @DisplayName("결제 실행 시 payment 상태를 성공으로 전환한다")
    void executesPayment() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);
        payment.markCreated("pay-token");

        LocalDateTime approvalTime = LocalDateTime.parse("2026-05-25T12:00:00");

        given(paymentRepository.findByIdAndMakerId(20L, 1L)).willReturn(Optional.of(payment));
        given(mockPaymentGateway.executePayment(any(TossPaymentExecuteRequest.class)))
                .willReturn(new TossPaymentExecuteResponse(
                        "order-no",
                        30000,
                        approvalTime,
                        30000,
                        PayMethod.TOSS_MONEY,
                        "pay-token",
                        "tx-1",
                        "092",
                        null
                ));
        Payment succeededPayment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(succeededPayment, "id", 20L);
        succeededPayment.markSucceeded("tx-1", 30000, PayMethod.TOSS_MONEY, "092", null, approvalTime);
        given(paymentExecuteStateService.markSucceeded(
                20L, 1L, "tx-1", 30000, PayMethod.TOSS_MONEY, "092", null, approvalTime
        )).willReturn(succeededPayment);
        given(testPublishService.publish(20L)).willReturn(99L);
        PaymentExecuteResponse response = mockPaymentService.executePayment(20L, 1L);

        assertThat(response.testId()).isEqualTo(99L);
        assertThat(response.payStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
        assertThat(response.transactionId()).isEqualTo("tx-1");
        assertThat(response.paidAmount()).isEqualTo(30000);
        assertThat(response.approvalTime()).isEqualTo(approvalTime);
        verify(testPublishService).publish(20L);
    }

    @Test
    @DisplayName("mock gateway가 0원을 반환해도 내부 payment 금액으로 paidAmount를 보정한다")
    void executesPaymentWithFallbackPaidAmount() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);
        payment.markCreated("pay-token");

        LocalDateTime approvalTime = LocalDateTime.parse("2026-05-25T12:00:00");

        given(paymentRepository.findByIdAndMakerId(20L, 1L)).willReturn(Optional.of(payment));
        given(mockPaymentGateway.executePayment(any(TossPaymentExecuteRequest.class)))
                .willReturn(new TossPaymentExecuteResponse(
                        "order-no",
                        0,
                        approvalTime,
                        0,
                        PayMethod.TOSS_MONEY,
                        "pay-token",
                        "tx-1",
                        "092",
                        null
                ));
        Payment succeededPayment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(succeededPayment, "id", 20L);
        succeededPayment.markSucceeded("tx-1", 30000, PayMethod.TOSS_MONEY, "092", null, approvalTime);
        given(paymentExecuteStateService.markSucceeded(
                20L, 1L, "tx-1", 30000, PayMethod.TOSS_MONEY, "092", null, approvalTime
        )).willReturn(succeededPayment);
        given(testPublishService.publish(20L)).willReturn(99L);

        PaymentExecuteResponse response = mockPaymentService.executePayment(20L, 1L);

        assertThat(response.testId()).isEqualTo(99L);
        assertThat(response.paidAmount()).isEqualTo(30000);
    }

    @Test
    @DisplayName("이미 publish까지 완료된 결제 실행 재호출 시 기존 결과를 반환한다")
    void returnsExistingExecutionResultWhenAlreadyPublished() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .testId(99L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .paidAmount(30000)
                .payMethod(PayMethod.TOSS_MONEY)
                .transactionId("tx-1")
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .isTestPayment(true)
                .approvalTime(LocalDateTime.parse("2026-05-25T12:00:00"))
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);

        given(paymentRepository.findByIdAndMakerId(20L, 1L)).willReturn(Optional.of(payment));

        PaymentExecuteResponse response = mockPaymentService.executePayment(20L, 1L);

        assertThat(response.testId()).isEqualTo(99L);
        assertThat(response.payStatus()).isEqualTo(PayStatus.PAY_SUCCEEDED);
    }

    @Test
    @DisplayName("결제는 성공했지만 publish가 안 된 상태면 execute 재호출 시 publish를 재시도한다")
    void retriesPublishWhenPaymentAlreadySucceeded() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .paidAmount(30000)
                .payMethod(PayMethod.TOSS_MONEY)
                .transactionId("tx-1")
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .isTestPayment(true)
                .approvalTime(LocalDateTime.parse("2026-05-25T12:00:00"))
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);

        given(paymentRepository.findByIdAndMakerId(20L, 1L)).willReturn(Optional.of(payment));
        given(testPublishService.publish(20L)).willReturn(99L);

        PaymentExecuteResponse response = mockPaymentService.executePayment(20L, 1L);

        assertThat(response.testId()).isEqualTo(99L);
        verify(testPublishService).publish(20L);
    }

    @Test
    @DisplayName("환불 시 환불 이력을 저장하고 payment 상태를 갱신한다")
    void refundsPayment() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);
        payment.markCreated("pay-token");
        payment.markSucceeded("tx-1", 30000, PayMethod.TOSS_MONEY, "092", null, LocalDateTime.parse("2026-05-25T12:00:00"));

        LocalDateTime refundTime = LocalDateTime.parse("2026-05-26T09:00:00");

        given(paymentRepository.findByIdAndMakerId(20L, 1L)).willReturn(Optional.of(payment));
        given(paymentRefundStateService.markRefundPending(20L, 1L)).willReturn(payment);
        given(mockPaymentGateway.refundPayment(any(TossPaymentRefundRequest.class)))
                .willReturn(new TossPaymentRefundResponse("refund-1", refundTime, 30000, "pay-token", "refund-tx-1"));
        Payment refundedPayment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("order-no")
                .payToken("pay-token")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .isTestPayment(true)
                .payStatus(PayStatus.REFUNDED)
                .build();
        ReflectionTestUtils.setField(refundedPayment, "id", 20L);
        given(paymentRefundStateService.markRefunded(20L, 1L, "테스트 환불",
                new TossPaymentRefundResponse("refund-1", refundTime, 30000, "pay-token", "refund-tx-1")))
                .willReturn(refundedPayment);

        PaymentRefundResponse response = mockPaymentService.refundPayment(20L, 1L, "테스트 환불");

        assertThat(response.payStatus()).isEqualTo(PayStatus.REFUNDED);
        assertThat(response.refundNo()).isEqualTo("refund-1");
    }
}
