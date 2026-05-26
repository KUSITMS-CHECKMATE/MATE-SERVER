package server.MATE.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.promotion.entity.PromotionReward;

public interface PromotionRewardRepository extends JpaRepository<PromotionReward, Long> {

    Optional<PromotionReward> findByParticipationId(Long participationId);
}
