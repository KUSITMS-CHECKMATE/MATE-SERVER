package server.MATE.domain.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.payment.policy.PaymentAmountCalculator;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentPrepareServiceTest {

    @Mock
    private TestDraftRepository testDraftRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAmountCalculator paymentAmountCalculator;

    @Mock
    private TestDraftValidator testDraftValidator;

    private PaymentPrepareService paymentPrepareService;

    @BeforeEach
    void setUp() {
        paymentPrepareService = new PaymentPrepareService(
                testDraftRepository,
                paymentRepository,
                paymentAmountCalculator,
                testDraftValidator
        );
    }

    @Test
    @DisplayName("금액 산정 필수값이 부족하면 결제 등록을 중단하고 DRAFT_005를 전파한다")
    void throwsDraft005WhenAmountFieldsMissing() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));
        given(testDraftValidator.validateForPayment(draft))
                .willThrow(new BaseException(BaseErrorCode.DRAFT_005));

        assertThatThrownBy(() -> paymentPrepareService.prepare(10L, 1L, true))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_005);

        verify(paymentAmountCalculator, never()).totalAmount(anyInt(), anyInt());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("게시 필수값이 부족하면 결제 등록을 중단하고 DRAFT_006을 전파한다")
    void throwsDraft006WhenPublishableFieldsMissing() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(5)
                .reward(300)
                .closedAt(LocalDateTime.now().plusDays(3))
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));
        given(testDraftValidator.validateForPayment(draft))
                .willThrow(new BaseException(BaseErrorCode.DRAFT_006));

        assertThatThrownBy(() -> paymentPrepareService.prepare(10L, 1L, true))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_006);

        verify(paymentAmountCalculator, never()).totalAmount(anyInt(), anyInt());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("질문 payload 구조가 잘못되면 결제 등록을 중단하고 payment를 생성하지 않는다")
    void blocksPaymentCreationWhenQuestionPayloadMalformed() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .title("제목")
                .description("설명")
                .categories(List.of("FOOD"))
                .goalPpl(5)
                .reward(300)
                .closedAt(LocalDateTime.now().plusDays(3))
                .questionsPayload(Map.of("questions", "not-a-list"))
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));
        given(testDraftValidator.validateForPayment(draft))
                .willThrow(new BaseException(BaseErrorCode.DRAFT_006));

        assertThatThrownBy(() -> paymentPrepareService.prepare(10L, 1L, true))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_006);

        verify(paymentAmountCalculator, never()).totalAmount(anyInt(), anyInt());
        verify(paymentRepository, never()).findByDraftId(anyLong());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("정상 초안이면 validator를 거친 뒤 결제 준비 정보를 반환한다")
    void preparesPaymentWhenDraftIsValid() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .title("제목")
                .description("설명")
                .categories(List.of("FOOD"))
                .goalPpl(5)
                .reward(300)
                .closedAt(LocalDateTime.now().plusDays(3))
                .questionsPayload(Map.of("questions", List.of(
                        Map.of("type", "SUBJECTIVE", "title", "질문 제목", "description", "질문 설명")
                )))
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        Payment savedPayment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderNo("temp-order")
                .goalPpl(5)
                .reward(300)
                .amount(1500)
                .isTestPayment(true)
                .build();
        ReflectionTestUtils.setField(savedPayment, "id", 20L);

        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));
        given(paymentAmountCalculator.totalAmount(5, 300)).willReturn(1500);
        given(paymentRepository.findByDraftId(10L)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willReturn(savedPayment);

        PaymentPrepareService.PaymentPreparation result = paymentPrepareService.prepare(10L, 1L, true);

        assertThat(result.paymentId()).isEqualTo(20L);
        assertThat(result.draftId()).isEqualTo(10L);
        assertThat(result.amount()).isEqualTo(1500);
        assertThat(result.orderNo()).startsWith("d-10-");
        verify(testDraftValidator).validateForPayment(draft);
    }
}
