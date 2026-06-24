package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.policy.RefundPolicy;
import server.MATE.domain.payment.service.RefundService;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.event.TestCompleteEvent;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCloseProcessorTest {

    @Mock private TestRepository testRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RefundPolicy refundPolicy;
    @Mock private RefundService refundService;

    private TestCloseProcessor processor;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        processor = new TestCloseProcessor(testRepository, eventPublisher, refundPolicy, refundService);
    }

    @Nested
    class AutoCloseTest {

        @Test
        @DisplayName("달성률 20% 이상이면 리포트 집계를 시작한다")
        void startsReportAggregationWhenAboveThreshold() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 25);
            when(testRepository.findByIdForUpdate(TEST_ID)).thenReturn(Optional.of(test));

            processor.process(TEST_ID);

            assertThat(test.getTestStatus()).isEqualTo(TestStatus.COMPLETED);
            ArgumentCaptor<TestCompleteEvent> captor = ArgumentCaptor.forClass(TestCompleteEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            verify(refundService, never()).requestRefund(any(), any());
        }

        @Test
        @DisplayName("달성률 20% 미만이고 환불 대상이면 환불을 요청한다")
        void requestsRefundWhenBelowThresholdAndEligible() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 10);
            when(testRepository.findByIdForUpdate(TEST_ID)).thenReturn(Optional.of(test));
            when(refundPolicy.isEligibleForRefund(test)).thenReturn(true);

            processor.process(TEST_ID);

            assertThat(test.getTestStatus()).isEqualTo(TestStatus.COMPLETED);
            verify(refundService).requestRefund(eq(TEST_ID), any(String.class));
            verify(eventPublisher, never()).publishEvent(any(TestCompleteEvent.class));
        }

        @Test
        @DisplayName("달성률 20% 미만이지만 환불 불가이면 종료만 한다")
        void justClosesWhenBelowThresholdButIneligible() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 10);
            when(testRepository.findByIdForUpdate(TEST_ID)).thenReturn(Optional.of(test));
            when(refundPolicy.isEligibleForRefund(test)).thenReturn(false);

            processor.process(TEST_ID);

            assertThat(test.getTestStatus()).isEqualTo(TestStatus.COMPLETED);
            verify(refundService, never()).requestRefund(any(), any());
            verify(eventPublisher, never()).publishEvent(any(TestCompleteEvent.class));
        }

        @Test
        @DisplayName("이미 종료된 테스트는 스킵한다")
        void skipsAlreadyCompletedTest() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 50);
            test.complete();
            when(testRepository.findByIdForUpdate(TEST_ID)).thenReturn(Optional.of(test));

            processor.process(TEST_ID);

            verify(refundService, never()).requestRefund(any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 테스트이면 TEST_004 예외를 던진다")
        void throwsWhenTestNotFound() {
            when(testRepository.findByIdForUpdate(TEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> processor.process(TEST_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getErrorCode())
                    .isEqualTo(BaseErrorCode.TEST_004);
        }
    }

    @Nested
    class ManualCloseTest {

        @Test
        @DisplayName("수동 종료 시 환불 불가 처리된다 (RefundPolicy가 false 반환)")
        void manualCloseIsNotEligibleForRefund() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 5);
            test.markClosedByMaker();
            when(refundPolicy.isEligibleForRefund(test)).thenReturn(false);

            processor.processClose(test);

            assertThat(test.getTestStatus()).isEqualTo(TestStatus.COMPLETED);
            verify(refundService, never()).requestRefund(any(), any());
        }

        @Test
        @DisplayName("수동 종료 + 달성률 20% 이상이면 리포트 집계가 시작된다")
        void manualCloseWithEnoughDataStartsReport() {
            server.MATE.domain.test.entity.Test test = inProgressTest(100, 30);
            test.markClosedByMaker();

            processor.processClose(test);

            assertThat(test.getTestStatus()).isEqualTo(TestStatus.COMPLETED);
            verify(eventPublisher).publishEvent(any(TestCompleteEvent.class));
        }
    }

    private server.MATE.domain.test.entity.Test inProgressTest(int goalPpl, long pplCount) {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .goalPpl(goalPpl)
                .reward(500)
                .testStatus(TestStatus.IN_PROGRESS)
                .closedAt(LocalDateTime.now().plusDays(30))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        ReflectionTestUtils.setField(test, "pplCount", pplCount);
        return test;
    }
}
