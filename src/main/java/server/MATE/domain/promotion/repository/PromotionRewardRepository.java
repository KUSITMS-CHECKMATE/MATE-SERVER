package server.MATE.domain.promotion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
            """)
    int clearTossUserKeyByTesterId(@Param("testerId") Long testerId);
}
