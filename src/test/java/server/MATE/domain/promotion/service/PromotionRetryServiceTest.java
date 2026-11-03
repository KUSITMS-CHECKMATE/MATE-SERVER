package server.MATE.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.promotion.dto.response.PromotionRetryResponse;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class PromotionRetryServiceTest {

    @Mock
    private PromotionRewardRepository promotionRewardRepository;

    @Mock
    private PromotionService promotionService;

    private PromotionRetryService promotionRetryService;

    @BeforeEach
    void setUp() {
        promotionRetryService = new PromotionRetryService(promotionRewardRepository, promotionService);
    }

    @Test
    @DisplayName("지급되지 않은 것이 확실한 FAILED 리워드는 READY로 되돌린 뒤 기존 지급 흐름으로 다시 지급한다")
    void retriesFailedReward() {
        given(promotionRewardRepository.findById(1L))
                .willReturn(Optional.of(reward(PromotionRewardStatus.FAILED, "TOSS_011")))
                .willReturn(Optional.of(reward(PromotionRewardStatus.SUCCEEDED, null)));
        given(promotionRewardRepository.resetFailedToReady(1L, PromotionRetryService.RETRYABLE_ERROR_CODES)).willReturn(1);

        PromotionRetryResponse response = promotionRetryService.retry(1L);

        verify(promotionService).grant(10L, 20L, 30L, 100);
        assertThat(response.status()).isEqualTo(PromotionRewardStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("재지급이 로컬에서 다시 실패하면 FAILED 상태와 실패 사유를 응답한다")
    void returnsFailedStatusWhenRetryFailsAgain() {
        given(promotionRewardRepository.findById(1L))
                .willReturn(Optional.of(reward(PromotionRewardStatus.FAILED, "TOSS_011")))
                .willReturn(Optional.of(reward(PromotionRewardStatus.FAILED, "TOSS_ACCOUNT_NOT_LINKED")));
        given(promotionRewardRepository.resetFailedToReady(1L, PromotionRetryService.RETRYABLE_ERROR_CODES)).willReturn(1);

        PromotionRetryResponse response = promotionRetryService.retry(1L);

        assertThat(response.status()).isEqualTo(PromotionRewardStatus.FAILED);
        assertThat(response.lastErrorCode()).isEqualTo("TOSS_ACCOUNT_NOT_LINKED");
    }

    @Test
    @DisplayName("FAILED가 아니면 재지급하지 않고 PROMOTION_002를 던진다")
    void rejectsNonFailedReward() {
        given(promotionRewardRepository.findById(1L))
                .willReturn(Optional.of(reward(PromotionRewardStatus.SUCCEEDED, null)));

        assertErrorCode(BaseErrorCode.PROMOTION_002);
        verify(promotionRewardRepository, never()).resetFailedToReady(anyLong(), any());
        verify(promotionService, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("지급 여부가 불확실한 실패 사유면 재지급하지 않고 PROMOTION_003을 던진다")
    void rejectsUncertainFailure() {
        given(promotionRewardRepository.findById(1L))
                .willReturn(Optional.of(reward(PromotionRewardStatus.FAILED, "PROMOTION_GATEWAY_ERROR")));

        assertErrorCode(BaseErrorCode.PROMOTION_003);
        verify(promotionRewardRepository, never()).resetFailedToReady(anyLong(), any());
        verify(promotionService, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("동시 요청으로 상태 전환에 실패하면 재지급하지 않고 PROMOTION_002를 던진다")
    void rejectsWhenAlreadyClaimed() {
        given(promotionRewardRepository.findById(1L))
                .willReturn(Optional.of(reward(PromotionRewardStatus.FAILED, "TOSS_011")));
        given(promotionRewardRepository.resetFailedToReady(1L, PromotionRetryService.RETRYABLE_ERROR_CODES)).willReturn(0);

        assertErrorCode(BaseErrorCode.PROMOTION_002);
        verify(promotionService, never()).grant(any(), any(), any(), any());
    }

    @Test
    @DisplayName("리워드가 없으면 PROMOTION_001을 던진다")
    void throwsWhenRewardNotFound() {
        given(promotionRewardRepository.findById(1L)).willReturn(Optional.empty());

        assertErrorCode(BaseErrorCode.PROMOTION_001);
        verify(promotionRewardRepository, never()).resetFailedToReady(anyLong(), any());
        verify(promotionService, never()).grant(any(), any(), any(), any());
    }

    private void assertErrorCode(BaseErrorCode expected) {
        assertThatThrownBy(() -> promotionRetryService.retry(1L))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getErrorCode()).isEqualTo(expected));
    }

    private PromotionReward reward(PromotionRewardStatus status, String lastErrorCode) {
        return PromotionReward.builder()
                .participationId(10L)
                .testId(20L)
                .testerId(30L)
                .rewardAmount(100)
                .status(status)
                .lastErrorCode(lastErrorCode)
                .build();
    }
}
