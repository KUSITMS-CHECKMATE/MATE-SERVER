package server.MATE.domain.promotion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "promotion_reward",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "participation_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionReward extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participation_id", nullable = false, unique = true)
    private Long participationId;

    @Column(nullable = false)
    private Long testId;

    @Column(nullable = false)
    private Long testerId;

    private Long tossUserKey;

    @Column(nullable = false)
    private Integer rewardAmount;

    @Column(length = 100)
    private String promotionCode;

    @Column(length = 500)
    private String rewardKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionRewardStatus status;

    @Column(length = 50)
    private String lastErrorCode;

    @Column(length = 500)
    private String lastErrorReason;

    private LocalDateTime executedAt;

    private LocalDateTime resolvedAt;

    @Builder
    public PromotionReward(
            Long participationId,
            Long testId,
            Long testerId,
            Long tossUserKey,
            Integer rewardAmount,
            String promotionCode,
            String rewardKey,
            PromotionRewardStatus status,
            String lastErrorCode,
            String lastErrorReason,
            LocalDateTime executedAt,
            LocalDateTime resolvedAt
    ) {
        this.participationId = participationId;
        this.testId = testId;
        this.testerId = testerId;
        this.tossUserKey = tossUserKey;
        this.rewardAmount = rewardAmount;
        this.promotionCode = promotionCode;
        this.rewardKey = rewardKey;
        this.status = status == null ? PromotionRewardStatus.READY : status;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorReason = lastErrorReason;
        this.executedAt = executedAt;
        this.resolvedAt = resolvedAt;
    }

    public boolean isInFlightOrCompleted() {
        return status == PromotionRewardStatus.KEY_ISSUED
                || status == PromotionRewardStatus.EXECUTED
                || status == PromotionRewardStatus.PENDING
                || status == PromotionRewardStatus.SUCCEEDED;
    }

    public void assignTossUserKey(Long tossUserKey) {
        this.tossUserKey = tossUserKey;
    }

    public void issueKey(String promotionCode, String rewardKey) {
        this.promotionCode = promotionCode;
        this.rewardKey = rewardKey;
        this.status = PromotionRewardStatus.KEY_ISSUED;
        this.lastErrorCode = null;
        this.lastErrorReason = null;
        this.resolvedAt = null;
    }

    public void markExecuted(LocalDateTime executedAt) {
        this.status = PromotionRewardStatus.EXECUTED;
        this.executedAt = executedAt;
    }

    public void markPending() {
        this.status = PromotionRewardStatus.PENDING;
        this.resolvedAt = null;
    }

    public void markSucceeded(LocalDateTime resolvedAt) {
        this.status = PromotionRewardStatus.SUCCEEDED;
        this.resolvedAt = resolvedAt;
        this.lastErrorCode = null;
        this.lastErrorReason = null;
    }

    public void markFailed(String errorCode, String errorReason, LocalDateTime resolvedAt) {
        this.status = PromotionRewardStatus.FAILED;
        this.lastErrorCode = errorCode;
        this.lastErrorReason = errorReason;
        this.resolvedAt = resolvedAt;
    }
}
