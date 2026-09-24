package server.MATE.global.discord.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;

public interface DiscordOutboxRepository extends JpaRepository<DiscordOutbox, Long> {

    Optional<DiscordOutbox> findByTypeAndTargetId(DiscordOutboxType type, Long targetId);

    List<DiscordOutbox> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            DiscordOutboxStatus status, LocalDateTime now);
}
