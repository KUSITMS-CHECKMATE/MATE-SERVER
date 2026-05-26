package server.MATE.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

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
import server.MATE.domain.users.entity.TossAccount;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.TossAccountRepository;
import server.MATE.toss.config.TossPromotionProperties;

@ExtendWith(MockitoExtension.class)
class PromotionPrepareServiceTest {

    @Mock
    private PromotionRewardRepository promotionRewardRepository;

    @Mock
    private TossAccountRepository tossAccountRepository;

    private PromotionPrepareService promotionPrepareService;

    @BeforeEach
    void setUp() {
        promotionPrepareService = new PromotionPrepareService(
                promotionRewardRepository,
                tossAccountRepository,
                new TossPromotionProperties(new TossPromotionProperties.Answer(true, "promo-answer"))
        );
    }

    @Test
    @DisplayName("enabled가 false면 skip한다")
    void skipsWhenDisabled() {
        PromotionPrepareService service = new PromotionPrepareService(
                promotionRewardRepository,
                tossAccountRepository,
                new TossPromotionProperties(new TossPromotionProperties.Answer(false, "promo-answer"))
        );

        var result = service.prepare(10L, 20L, 30L, 300);

        assertThat(result.skipped()).isTrue();
    }

    @Test
    @DisplayName("진행 중이거나 완료된 지급 건이면 skip한다")
    void skipsWhenRewardAlreadyInFlightOrCompleted() {
        PromotionReward reward = PromotionReward.builder()
                .participationId(10L)
                .testId(20L)
                .testerId(30L)
                .rewardAmount(300)
                .status(PromotionRewardStatus.SUCCEEDED)
                .build();

        given(promotionRewardRepository.findByParticipationId(10L)).willReturn(Optional.of(reward));

        var result = promotionPrepareService.prepare(10L, 20L, 30L, 300);

        assertThat(result.skipped()).isTrue();
    }

    @Test
    @DisplayName("linked toss account가 없으면 failure를 반환한다")
    void failsWhenTossAccountIsMissing() {
        PromotionReward reward = PromotionReward.builder()
                .participationId(10L)
                .testId(20L)
                .testerId(30L)
                .rewardAmount(300)
                .build();
        ReflectionTestUtils.setField(reward, "id", 1L);

        given(promotionRewardRepository.findByParticipationId(10L)).willReturn(Optional.empty());
        given(promotionRewardRepository.save(any(PromotionReward.class))).willReturn(reward);
        given(tossAccountRepository.findByUserId(30L)).willReturn(Optional.empty());

        var result = promotionPrepareService.prepare(10L, 20L, 30L, 300);

        assertThat(result.shouldFail()).isTrue();
        assertThat(result.errorCode()).isEqualTo(PromotionPrepareService.LOCAL_ERROR_CODE_NOT_LINKED);
    }

    @Test
    @DisplayName("준비가 완료되면 rewardId, tossUserKey, promotionCode를 반환한다")
    void returnsReadyPreparation() {
        PromotionReward reward = PromotionReward.builder()
                .participationId(10L)
                .testId(20L)
                .testerId(30L)
                .rewardAmount(300)
                .build();
        ReflectionTestUtils.setField(reward, "id", 1L);

        TossAccount tossAccount = TossAccount.builder()
                .user(Users.builder().ci("ci").name("tester").build())
                .tossUserKey(777L)
                .isLinked(true)
                .build();

        given(promotionRewardRepository.findByParticipationId(10L)).willReturn(Optional.empty());
        given(promotionRewardRepository.save(any(PromotionReward.class))).willReturn(reward);
        given(tossAccountRepository.findByUserId(30L)).willReturn(Optional.of(tossAccount));

        var result = promotionPrepareService.prepare(10L, 20L, 30L, 300);

        assertThat(result.ready()).isTrue();
        assertThat(result.rewardId()).isEqualTo(1L);
        assertThat(result.tossUserKey()).isEqualTo(777L);
        assertThat(result.promotionCode()).isEqualTo("promo-answer");
        assertThat(reward.getTossUserKey()).isEqualTo(777L);
    }
}
