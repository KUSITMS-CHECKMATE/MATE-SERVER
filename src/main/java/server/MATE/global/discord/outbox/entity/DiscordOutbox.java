package server.MATE.global.discord.outbox.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(
        name = "discord_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_discord_outbox_type_target", columnNames = {"type", "target_id"})
        },
        indexes = {
                @Index(name = "idx_discord_outbox_status_next_attempt", columnList = "status, next_attempt_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordOutbox extends BaseEntity {

    // 첫 시도를 포함한 최대 시도 횟수
    public static final int MAX_ATTEMPTS = 5;

    private static final int LAST_ERROR_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscordOutboxType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscordOutboxStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    private LocalDateTime sentAt;

    private DiscordOutbox(DiscordOutboxType type, Long targetId, LocalDateTime nextAttemptAt) {
        this.type = type;
        this.targetId = targetId;
        this.status = DiscordOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = nextAttemptAt;
    }

    // 초기 다음 시도 시각을 1분 뒤로 설정해 커밋 직후 즉시 시도와 재시도 스케줄러의 중복 방지
    public static DiscordOutbox pending(DiscordOutboxType type, Long targetId, LocalDateTime now) {
        return new DiscordOutbox(type, targetId, now.plusMinutes(1));
    }

    public boolean isPending() {
        return status == DiscordOutboxStatus.PENDING;
    }

    public void markSent(LocalDateTime now) {
        this.status = DiscordOutboxStatus.SENT;
        this.sentAt = now;
    }

    public void markSkipped() {
        this.status = DiscordOutboxStatus.SKIPPED;
    }

    // 실패 시 1·2·4·8분 간격으로 연기, MAX_ATTEMPTS회째 실패 시 FAILED 처리
    public void recordFailure(String error, LocalDateTime now) {
        this.attemptCount++;
        this.lastError = truncate(error);
        if (attemptCount >= MAX_ATTEMPTS) {
            this.status = DiscordOutboxStatus.FAILED;
            return;
        }
        this.nextAttemptAt = now.plusMinutes(1L << (attemptCount - 1));
    }

    private static String truncate(String error) {
        if (error == null || error.length() <= LAST_ERROR_MAX_LENGTH) {
            return error;
        }
        return error.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
