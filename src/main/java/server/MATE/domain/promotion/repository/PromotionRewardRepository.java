package server.MATE.domain.promotion.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.promotion.entity.PromotionReward;
import server.MATE.domain.promotion.entity.PromotionRewardStatus;

public interface PromotionRewardRepository extends JpaRepository<PromotionReward, Long> {

    Optional<PromotionReward> findByParticipationId(Long participationId);

    List<PromotionReward> findTop100ByStatusIn(List<PromotionRewardStatus> statuses);

    @Query("""
            select coalesce(sum(pr.rewardAmount), 0)
            from PromotionReward pr
            where pr.testerId = :testerId
              and pr.status = :status
            """)
    Long sumRewardAmountByTesterIdAndStatus(@Param("testerId") Long testerId,
                                            @Param("status") PromotionRewardStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from PromotionReward pr
            where pr.testId = :testId
            """)
    int deleteAllByTestId(@Param("testId") Long testId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PromotionReward pr
            set pr.tossUserKey = null
            where pr.testerId = :testerId
              and pr.status not in (
                  server.MATE.domain.promotion.entity.PromotionRewardStatus.READY,
                  server.MATE.domain.promotion.entity.PromotionRewardStatus.KEY_ISSUED,
                  server.MATE.domain.promotion.entity.PromotionRewardStatus.PENDING,
                  server.MATE.domain.promotion.entity.PromotionRewardStatus.EXECUTED
              )
            """)
    int clearTossUserKeyByTesterId(@Param("testerId") Long testerId);

    // 재지급 동시 호출 시 한 건만 통과하도록, 재지급 가능한 사유로 FAILED인 경우에만 READY로 되돌린다.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PromotionReward pr
            set pr.status = server.MATE.domain.promotion.entity.PromotionRewardStatus.READY
            where pr.id = :rewardId
              and pr.status = server.MATE.domain.promotion.entity.PromotionRewardStatus.FAILED
              and pr.lastErrorCode in :retryableErrorCodes
            """)
    int resetFailedToReady(@Param("rewardId") Long rewardId,
                           @Param("retryableErrorCodes") Collection<String> retryableErrorCodes);
}
