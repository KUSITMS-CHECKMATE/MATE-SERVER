package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.entity.PayMethod;
import server.MATE.domain.payment.entity.PayStatus;
import server.MATE.domain.payment.entity.Payment;
import server.MATE.domain.payment.repository.PaymentRepository;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.service.QuestionService;
import server.MATE.domain.test.event.TestCreatedEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestPublishServiceTest {

    @Mock
    private TestDraftRepository testDraftRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TestDraftValidator testDraftValidator;

    private TestPublishService testPublishService;

    @BeforeEach
    void setUp() {
        testPublishService = new TestPublishService(
                testDraftRepository,
                paymentRepository,
                testRepository,
                questionService,
                eventPublisher,
                testDraftValidator
        );
    }

    @Test
    @DisplayName("결제 성공 후 draft를 실제 test와 question으로 publish한다")
    void publishesDraft() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .title("테스트 제목")
                .description("테스트 설명")
                .serviceName("서비스명")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of("image-1"))
                .categories(List.of("DAILY", "FINANCE"))
                .goalPpl(100)
                .reward(300)
                .closedAt(LocalDateTime.parse("2099-05-31T23:59:59"))
                .questionsPayload(Map.of(
                        "questions", List.of(
                                Map.of("type", "SUBJECTIVE", "title", "질문 제목", "description", "질문 설명")
                        )
                ))
                .status(TestDraftStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .orderId("order-no")
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .payMethod(PayMethod.IN_APP_PURCHASE)
                .approvedAt(LocalDateTime.parse("2026-05-25T12:00:00"))
                .build();

        ReflectionTestUtils.setField(payment, "id", 20L);

        given(paymentRepository.findByIdForUpdate(20L)).willReturn(Optional.of(payment));
        given(testDraftRepository.findByIdForUpdate(10L)).willReturn(Optional.of(draft));
        given(testDraftValidator.validateForPublish(any(TestDraft.class)))
                .willReturn(new QuestionCreateRequest(List.of()));
        given(testRepository.save(any(server.MATE.domain.test.entity.Test.class))).willAnswer(invocation -> {
            server.MATE.domain.test.entity.Test saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });

        Long testId = testPublishService.publish(20L);

        assertThat(testId).isEqualTo(99L);
        assertThat(payment.getTestId()).isEqualTo(99L);
        assertThat(draft.getPublishedTestId()).isEqualTo(99L);
        assertThat(draft.getStatus()).isEqualTo(TestDraftStatus.PUBLISHED);

        ArgumentCaptor<server.MATE.domain.test.entity.Test> testCaptor =
                ArgumentCaptor.forClass(server.MATE.domain.test.entity.Test.class);
        verify(testRepository).save(testCaptor.capture());
        server.MATE.domain.test.entity.Test savedTest = testCaptor.getValue();
        assertThat(savedTest.getGoalPpl()).isEqualTo(100);
        assertThat(savedTest.getReward()).isEqualTo(300);
        assertThat(savedTest.getClosedAt()).isEqualTo(LocalDateTime.parse("2099-05-31T23:59:59"));
        assertThat(savedTest.getCategories()).hasSize(2);

        verify(questionService).createQuestions(any(Long.class), any(Long.class), any(QuestionCreateRequest.class));
        verify(testDraftRepository).delete(draft);

        ArgumentCaptor<TestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TestCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TestCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.testId()).isEqualTo(99L);
        assertThat(publishedEvent.title()).isEqualTo("테스트 제목");
        assertThat(publishedEvent.reward()).isEqualTo(300);
        assertThat(publishedEvent.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("payment에 이미 testId가 연결되어 있으면 draft 없이 기존 testId를 그대로 반환한다")
    void returnsExistingLinkedTestIdWithoutDraft() {
        Payment payment = Payment.builder()
                .draftId(10L)
                .testId(99L)
                .makerId(1L)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .build();

        ReflectionTestUtils.setField(payment, "id", 20L);

        given(paymentRepository.findByIdForUpdate(20L)).willReturn(Optional.of(payment));

        Long testId = testPublishService.publish(20L);

        assertThat(testId).isEqualTo(99L);
        assertThat(payment.getTestId()).isEqualTo(99L);
        verify(testDraftRepository, never()).findByIdForUpdate(any(Long.class));
    }

    @Test
    @DisplayName("TestDraftValidator가 DRAFT_006을 던지면 publish 단계에서 그대로 전파된다")
    void throwsDraft006WhenTestDraftValidatorFails() {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .title("테스트 제목")
                .description("테스트 설명")
                .categories(List.of("DAILY"))
                .goalPpl(100)
                .reward(300)
                .closedAt(LocalDateTime.parse("2099-05-31T23:59:59"))
                .questionsPayload(Map.of("questions", List.of(Map.of("type", "SUBJECTIVE", "title", "질문"))))
                .status(TestDraftStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);

        Payment payment = Payment.builder()
                .draftId(10L)
                .makerId(1L)
                .payStatus(PayStatus.PAY_SUCCEEDED)
                .goalPpl(100)
                .reward(300)
                .amount(30000)
                .build();
        ReflectionTestUtils.setField(payment, "id", 20L);

        given(paymentRepository.findByIdForUpdate(20L)).willReturn(Optional.of(payment));
        given(testDraftRepository.findByIdForUpdate(10L)).willReturn(Optional.of(draft));
        given(testDraftValidator.validateForPublish(any(TestDraft.class)))
                .willThrow(new BaseException(BaseErrorCode.DRAFT_006));

        assertThatThrownBy(() -> testPublishService.publish(20L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("게시에 필요한 필수 정보가 부족합니다.");
    }
}
