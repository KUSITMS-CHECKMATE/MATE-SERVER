package server.MATE.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;
import server.MATE.domain.promotion.repository.PromotionRewardRepository;
import server.MATE.domain.promotion.service.PromotionPendingResolverService;
import server.MATE.global.storage.service.FileStorageService;
import server.MATE.toss.gateway.PromotionGatewayExecutionStatus;
import server.MATE.toss.gateway.TossPromotionGateway;

@SpringBootTest
@ActiveProfiles("test")
class PromotionPendingResolverIntegrationTest {

    @Autowired
    private PromotionPendingResolverService promotionPendingResolverService;

    @Autowired
    private PromotionRewardRepository promotionRewardRepository;

    @MockitoBean
    private TossPromotionGateway tossPromotionGateway;

    @MockitoBean
    private FileStorageService fileStorageService;

    @AfterEach
    void tearDown() {
        promotionRewardRepository.deleteAll();
    }

    @Test
    @DisplayName("PENDING 리워드가 재조회 결과 SUCCEEDED면 실제 DB 상태가 SUCCEEDED로 바뀐다")
    void resolvesPendingRewardToSucceededInDatabase() {
        PromotionReward reward = promotionRewardRepository.save(PromotionReward.builder()
                .participationId(910001L)
                .testId(1L)
                .testerId(1L)
                .tossUserKey(777L)
                .rewardAmount(300)
                .promotionCode("promo-code")
                .rewardKey("reward-key-1")
                .status(PromotionRewardStatus.PENDING)
                .build());
        given(tossPromotionGateway.getExecutionStatus(777L, "promo-code", "reward-key-1"))
                .willReturn(PromotionGatewayExecutionStatus.SUCCEEDED);

        promotionPendingResolverService.resolveAll();

        PromotionReward resolved = promotionRewardRepository.findById(reward.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(PromotionRewardStatus.SUCCEEDED);
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("EXECUTED 리워드가 재조회 결과 FAILED면 실제 DB 상태가 FAILED로 바뀐다")
    void resolvesExecutedRewardToFailedInDatabase() {
        PromotionReward reward = promotionRewardRepository.save(PromotionReward.builder()
                .participationId(910002L)
                .testId(1L)
                .testerId(1L)
                .tossUserKey(778L)
                .rewardAmount(300)
                .promotionCode("promo-code")
                .rewardKey("reward-key-2")
                .status(PromotionRewardStatus.EXECUTED)
                .build());
        given(tossPromotionGateway.getExecutionStatus(778L, "promo-code", "reward-key-2"))
                .willReturn(PromotionGatewayExecutionStatus.FAILED);

        promotionPendingResolverService.resolveAll();

        PromotionReward resolved = promotionRewardRepository.findById(reward.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(PromotionRewardStatus.FAILED);
        assertThat(resolved.getLastErrorCode()).isEqualTo("PROMOTION_EXECUTION_FAILED");
    }
}
