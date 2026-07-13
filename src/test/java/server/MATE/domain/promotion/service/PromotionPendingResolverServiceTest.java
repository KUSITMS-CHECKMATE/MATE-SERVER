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
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

@ExtendWith(MockitoExtension.class)
class PromotionPendingResolverServiceTest {

    @Mock
    private PromotionRewardRepository promotionRewardRepository;

    @Mock
    private PromotionFailureStateService promotionFailureStateService;

    @Mock
    private PromotionExecutionResultApplier promotionExecutionResultApplier;

    @Mock
    private TossPromotionGateway tossPromotionGateway;

    private PromotionPendingResolverService resolverService;

    @BeforeEach
    void setUp() {
        resolverService = new PromotionPendingResolverService(
                promotionRewardRepository,
                promotionFailureStateService,
                promotionExecutionResultApplier,
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
    @DisplayName("PENDING 건의 결과를 gateway 상태 그대로 applier에 위임한다")
    void delegatesResultToApplierForPendingReward() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-1"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier).apply(1L, PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("결과가 FAILED여도 resolver는 직접 실패 처리하지 않고 applier에 위임한다")
    void delegatesFailedResultToApplier() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-1"))
                .willReturn(PromotionGatewayExecutionStatus.FAILED);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier).apply(1L, PromotionGatewayExecutionStatus.FAILED);
        verify(promotionFailureStateService, never()).markFailed(any(), any(), any());
    }

    @Test
    @DisplayName("EXECUTED 건도 gateway 상태를 applier에 위임한다")
    void delegatesResultForExecutedReward() {
        PromotionReward reward = executedReward(2L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-2"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier).apply(2L, PromotionGatewayExecutionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("이미 PENDING인 건이 여전히 PENDING이면 applier를 호출하지 않는다")
    void skipsApplierWhenStillPending() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-1"))
                .willReturn(PromotionGatewayExecutionStatus.PENDING);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier, never()).apply(any(), any());
    }

    @Test
    @DisplayName("EXECUTED 건이 PENDING으로 처음 전환되면 applier를 호출한다")
    void appliesFirstPendingTransitionForExecutedReward() {
        PromotionReward reward = executedReward(2L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-2"))
                .willReturn(PromotionGatewayExecutionStatus.PENDING);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier).apply(2L, PromotionGatewayExecutionStatus.PENDING);
    }

    @Test
    @DisplayName("특정 건에서 예외가 발생해도 나머지 건은 계속 처리한다")
    void continuesProcessingOnPartialFailure() {
        PromotionReward failingReward = pendingReward(1L);
        PromotionReward successReward = pendingReward(2L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(failingReward, successReward));
        given(tossPromotionGateway.getExecutionStatus(any(), any(), any()))
                .willThrow(new RuntimeException("Toss API 오류"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        resolverService.resolveAll();

        verify(promotionExecutionResultApplier).apply(2L, PromotionGatewayExecutionStatus.SUCCEEDED);
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
        verify(tossPromotionGateway, never()).getExecutionStatus(any(), any(), any());
        verify(promotionExecutionResultApplier, never()).apply(any(), any());
    }

    @Test
    @DisplayName("처리할 건이 없으면 gateway를 호출하지 않는다")
    void doesNothingWhenNoPendingRewards() {
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of());

        resolverService.resolveAll();

        verify(tossPromotionGateway, never()).getExecutionStatus(any(), any(), any());
    }

    @Test
    @DisplayName("gateway 호출 시 저장된 tossUserKey, promotionCode, rewardKey를 그대로 사용한다")
    void usesStoredCredentialsForGatewayCall() {
        PromotionReward reward = pendingReward(1L);
        given(promotionRewardRepository.findTop100ByStatusIn(any())).willReturn(List.of(reward));
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-answer", "reward-key-1"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        resolverService.resolveAll();

        verify(tossPromotionGateway).getExecutionStatus(777L, "promo-answer", "reward-key-1");
    }
}
