package server.MATE.domain.promotion.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.toss.dto.request.TossPromotionResultRequest;
import server.MATE.toss.dto.response.TossPromotionExecutionStatus;
import server.MATE.toss.dto.response.TossPromotionResultResponse;
import server.MATE.toss.gateway.TossPromotionGateway;

@ExtendWith(MockitoExtension.class)
class PromotionPendingResolverServiceTest {

    @Mock
    private PromotionRewardRepository promotionRewardRepository;

    @Mock
    private PromotionExecuteStateService promotionExecuteStateService;

    @Mock
    private PromotionFailureStateService promotionFailureStateService;

    @Mock
    private TossPromotionGateway tossPromotionGateway;

    private PromotionPendingResolverService resolverService;

    @BeforeEach
    void setUp() {
        resolverService = new PromotionPendingResolverService(
                promotionRewardRepository,
                promotionExecuteStateService,
                promotionFailureStateService,
                tossPromotionGateway
        );
    }

    private PromotionReward pendingReward(Long id) {
        PromotionReward reward = PromotionReward.builder()
                .participationId(id)
                .testId(1L)
                .testerId(1L)
                .tossUserKey(777L)
                .rewardAmount(300)
                .promotionCode("promo-answer")
                .rewardKey("reward-key-" + id)
                .status(PromotionRewardStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(reward, "id", id);
        return reward;
    }

    private PromotionReward executedReward(Long id) {
        PromotionReward reward = PromotionReward.builder()
                .participationId(id)
                .testId(1L)
                .testerId(1L)
                .tossUserKey(777L)
                .rewardAmount(300)
                .promotionCode("promo-answer")
                .rewardKey("reward-key-" + id)
                .status(PromotionRewardStatus.EXECUTED)
                .build();
        ReflectionTestUtils.setField(reward, "id", id);
        return reward;
    }

    @Test
    @DisplayName("PENDING 건의 결과가 SUCCESS면 SUCCEEDED로 업데이트한다")
    void resolvesSuccessFromPending() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS));

        resolverService.resolveAll();

        verify(promotionExecuteStateService).markSucceeded(1L);
        verify(promotionFailureStateService, never()).markFailed(any(), any(), any());
    }

    @Test
    @DisplayName("PENDING 건의 결과가 FAILED면 FAILED로 업데이트한다")
    void resolvesFailedFromPending() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.FAILED));

        resolverService.resolveAll();

        verify(promotionFailureStateService).markFailed(eq(1L), any(), any());
        verify(promotionExecuteStateService, never()).markSucceeded(any());
    }

    @Test
    @DisplayName("PENDING 건의 결과가 여전히 PENDING이면 상태를 변경하지 않는다")
    void doesNothingWhenStillPending() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.PENDING));

        resolverService.resolveAll();

        verify(promotionExecuteStateService, never()).markSucceeded(any());
        verify(promotionFailureStateService, never()).markFailed(any(), any(), any());
    }

    @Test
    @DisplayName("EXECUTED 건의 결과가 SUCCESS면 SUCCEEDED로 업데이트한다")
    void resolvesSuccessFromExecuted() {
        PromotionReward reward = executedReward(2L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS));

        resolverService.resolveAll();

        verify(promotionExecuteStateService).markSucceeded(2L);
    }

    @Test
    @DisplayName("특정 건에서 예외가 발생해도 나머지 건은 계속 처리한다")
    void continuesProcessingOnPartialFailure() {
        PromotionReward failingReward = pendingReward(1L);
        PromotionReward successReward = pendingReward(2L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(failingReward, successReward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willThrow(new RuntimeException("Toss API 오류"))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS));

        resolverService.resolveAll();

        verify(promotionExecuteStateService).markSucceeded(2L);
    }

    @Test
    @DisplayName("tossUserKey, promotionCode, rewardKey 중 하나라도 null이면 FAILED 처리한다")
    void marksFailedWhenRequiredFieldsAreMissing() {
        PromotionReward reward = PromotionReward.builder()
                .participationId(1L)
                .testId(1L)
                .testerId(1L)
                .rewardAmount(300)
                .status(PromotionRewardStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(reward, "id", 1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));

        resolverService.resolveAll();

        verify(promotionFailureStateService).markFailed(eq(1L), any(), any());
        verify(tossPromotionGateway, never()).getExecutionResult(any());
    }

    @Test
    @DisplayName("처리할 건이 없으면 gateway를 호출하지 않는다")
    void doesNothingWhenNoPendingRewards() {
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of());

        resolverService.resolveAll();

        verify(tossPromotionGateway, never()).getExecutionResult(any());
    }

    @Test
    @DisplayName("gateway 호출 시 저장된 tossUserKey, promotionCode, rewardKey를 그대로 사용한다")
    void usesStoredCredentialsForGatewayCall() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionResult(any(TossPromotionResultRequest.class)))
                .willReturn(new TossPromotionResultResponse(TossPromotionExecutionStatus.SUCCESS));

        resolverService.resolveAll();

        verify(tossPromotionGateway).getExecutionResult(
                new TossPromotionResultRequest(777L, "promo-answer", "reward-key-1")
        );
    }
}
