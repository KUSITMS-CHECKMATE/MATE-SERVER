package server.MATE.domain.testdraft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.payment.policy.IapProductTierCatalog;
import server.MATE.domain.testdraft.dto.request.TestDraftUpdateRequest;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.toss.config.TossIapProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestDraftServiceTest {

    @Mock
    private TestDraftRepository testDraftRepository;

    @Mock
    private IapProductTierCatalog iapProductTierCatalog;

    @Mock
    private TestDraftValidator testDraftValidator;

    private TestDraftService testDraftService;

    private static final Long MAKER_ID = 1L;
    private static final Long DRAFT_ID = 10L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        testDraftService = new TestDraftService(testDraftRepository, objectMapper, iapProductTierCatalog, testDraftValidator);
    }

    @Test
    @DisplayName("draft 조회 응답에는 더 이상 금액 내역이 포함되지 않는다")
    void getDraftResponseHasNoAmountBreakdown() {
        TestDraft draft = draftWith(30, 200, LocalDateTime.parse("2099-06-30T23:59:59"));
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));

        TestDraftResponse response = testDraftService.getDraft(DRAFT_ID, MAKER_ID);

        assertThat(response.goalPpl()).isEqualTo(30);
        assertThat(response.reward()).isEqualTo(200);
    }

    @Test
    @DisplayName("publishCheck: 상태/티어/게시필드가 모두 유효하면 예외 없이 통과한다")
    void publishCheckPassesWhenEverythingIsValid() {
        TestDraft draft = publishableDraftWith(100, 500);
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));
        given(iapProductTierCatalog.find(100, 500))
                .willReturn(Optional.of(new TossIapProperties.Tier(100, 500, "sku_100_500", 91740)));

        testDraftService.publishCheck(DRAFT_ID, MAKER_ID);

        verify(testDraftValidator).validateForPublish(draft);
    }

    @Test
    @DisplayName("publishCheck: PUBLISHED가 아니면서 잘못된 상태면 DRAFT_004 예외를 던진다")
    void publishCheckThrowsDraft004WhenStateInvalid() {
        TestDraft draft = publishableDraftWith(100, 500);
        ReflectionTestUtils.setField(draft, "status", TestDraftStatus.EXPIRED);
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> testDraftService.publishCheck(DRAFT_ID, MAKER_ID))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_004);
    }

    @Test
    @DisplayName("publishCheck: goalPpl/reward 조합이 티어 카탈로그에 없으면 DRAFT_007 예외를 던진다")
    void publishCheckThrowsDraft007WhenTierNotFound() {
        TestDraft draft = publishableDraftWith(13, 520);
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));
        given(iapProductTierCatalog.find(13, 520)).willReturn(Optional.empty());

        assertThatThrownBy(() -> testDraftService.publishCheck(DRAFT_ID, MAKER_ID))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_007);
    }

    @Test
    @DisplayName("publishCheck: 다른 사용자의 draft에 접근하면 DRAFT_002 예외를 던진다")
    void publishCheckThrowsDraft002WhenNotOwner() {
        TestDraft draft = publishableDraftWith(100, 500);
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> testDraftService.publishCheck(DRAFT_ID, 999L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_002);
    }

    @Test
    @DisplayName("updateDraft: goalPpl/reward가 병합된 후 티어와 일치하면 예외 없이 통과한다")
    void updateDraftPassesWhenMergedTierMatches() {
        TestDraft draft = draftWith(30, 200, LocalDateTime.parse("2099-06-30T23:59:59"));
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));
        given(iapProductTierCatalog.find(30, 200))
                .willReturn(Optional.of(new TossIapProperties.Tier(30, 200, "sku_30_200", 11000)));

        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                null, null, null, null, null, null, 30, 200, null, null
        );

        TestDraftResponse response = testDraftService.updateDraft(DRAFT_ID, MAKER_ID, request);

        assertThat(response.goalPpl()).isEqualTo(30);
        assertThat(response.reward()).isEqualTo(200);
    }

    @Test
    @DisplayName("updateDraft: goalPpl/reward가 병합된 후 티어와 불일치하면 DRAFT_007 예외를 던진다")
    void updateDraftThrowsDraft007WhenMergedTierNotFound() {
        TestDraft draft = draftWith(13, 520, LocalDateTime.parse("2099-06-30T23:59:59"));
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));
        given(iapProductTierCatalog.find(13, 520)).willReturn(Optional.empty());

        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                null, null, null, null, null, null, 13, 520, null, null
        );

        assertThatThrownBy(() -> testDraftService.updateDraft(DRAFT_ID, MAKER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.DRAFT_007);
    }

    @Test
    @DisplayName("updateDraft: 이번 요청에 reward만 있어도 DB에 저장된 goalPpl과 병합해서 티어를 검증한다")
    void updateDraftValidatesMergedGoalPplWhenOnlyRewardInRequest() {
        TestDraft draft = draftWith(30, 100, LocalDateTime.parse("2099-06-30T23:59:59"));
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));
        given(iapProductTierCatalog.find(30, 200))
                .willReturn(Optional.of(new TossIapProperties.Tier(30, 200, "sku_30_200", 11000)));

        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                null, null, null, null, null, null, null, 200, null, null
        );

        testDraftService.updateDraft(DRAFT_ID, MAKER_ID, request);

        verify(iapProductTierCatalog).find(30, 200);
    }

    @Test
    @DisplayName("updateDraft: goalPpl/reward가 병합 후에도 하나라도 null이면 티어 검증을 생략한다")
    void updateDraftSkipsTierValidationWhenEitherFieldStillNull() {
        TestDraft draft = TestDraft.builder()
                .makerId(MAKER_ID)
                .build();
        ReflectionTestUtils.setField(draft, "id", DRAFT_ID);
        given(testDraftRepository.findById(DRAFT_ID)).willReturn(Optional.of(draft));

        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                "수정된 제목", null, null, null, null, null, null, null, null, null
        );

        testDraftService.updateDraft(DRAFT_ID, MAKER_ID, request);

        verify(iapProductTierCatalog, never()).find(anyInt(), anyInt());
    }

    private TestDraft draftWith(int goalPpl, int reward, LocalDateTime closedAt) {
        TestDraft draft = TestDraft.builder()
                .makerId(MAKER_ID)
                .goalPpl(goalPpl)
                .reward(reward)
                .closedAt(closedAt)
                .build();
        ReflectionTestUtils.setField(draft, "id", DRAFT_ID);
        return draft;
    }

    private TestDraft publishableDraftWith(int goalPpl, int reward) {
        TestDraft draft = TestDraft.builder()
                .makerId(MAKER_ID)
                .title("제목")
                .description("설명")
                .categories(List.of("FOOD"))
                .goalPpl(goalPpl)
                .reward(reward)
                .closedAt(LocalDateTime.parse("2099-06-30T23:59:59"))
                .questionsPayload(Map.of(
                        "questions", List.of(
                                Map.of("type", "SUBJECTIVE", "title", "질문 제목", "description", "질문 설명")
                        )
                ))
                .status(TestDraftStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(draft, "id", DRAFT_ID);
        return draft;
    }
}
