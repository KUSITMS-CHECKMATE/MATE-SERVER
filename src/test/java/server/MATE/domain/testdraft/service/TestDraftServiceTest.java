package server.MATE.domain.testdraft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.policy.DefaultPaymentAmountCalculator;
import server.MATE.domain.payment.policy.PaymentAmountCalculator;
import server.MATE.domain.testdraft.dto.response.PaymentAmountResponse;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TestDraftServiceTest {

    @Mock
    private TestDraftRepository testDraftRepository;

    private TestDraftService testDraftService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PaymentAmountCalculator paymentAmountCalculator = new DefaultPaymentAmountCalculator();
        testDraftService = new TestDraftService(testDraftRepository, objectMapper, paymentAmountCalculator);
    }

    @Test
    @DisplayName("기간·인원·리워드가 모두 있으면 결제 금액 내역을 반환한다")
    void returnsAmountBreakdownWhenAllPaymentFieldsArePresent() {
        TestDraft draft = draftWith(30, 200, LocalDateTime.parse("2099-06-30T23:59:59"));
        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));

        TestDraftResponse response = testDraftService.getDraft(10L, 1L);

        PaymentAmountResponse amountBreakdown = response.amountBreakdown();
        assertThat(amountBreakdown.testerRewardAmount()).isEqualTo(6000);
        assertThat(amountBreakdown.feeAmount()).isEqualTo(4000);
        assertThat(amountBreakdown.vatAmount()).isEqualTo(1000);
        assertThat(amountBreakdown.totalAmount()).isEqualTo(11000);
    }

    @Test
    @DisplayName("closedAt이 없으면 결제 금액 내역을 반환하지 않는다")
    void returnsNullAmountBreakdownWhenClosedAtIsMissing() {
        TestDraft draft = draftWith(30, 200, null);
        given(testDraftRepository.findById(10L)).willReturn(Optional.of(draft));

        TestDraftResponse response = testDraftService.getDraft(10L, 1L);

        assertThat(response.amountBreakdown()).isNull();
    }

    private TestDraft draftWith(int goalPpl, int reward, LocalDateTime closedAt) {
        TestDraft draft = TestDraft.builder()
                .makerId(1L)
                .goalPpl(goalPpl)
                .reward(reward)
                .closedAt(closedAt)
                .build();
        ReflectionTestUtils.setField(draft, "id", 10L);
        return draft;
    }
}
