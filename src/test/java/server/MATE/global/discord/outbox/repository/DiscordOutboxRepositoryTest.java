package server.MATE.global.discord.outbox.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;
import server.MATE.global.discord.outbox.entity.DiscordOutbox;
import server.MATE.global.discord.outbox.entity.DiscordOutboxStatus;
import server.MATE.global.discord.outbox.entity.DiscordOutboxType;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class DiscordOutboxRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 24, 10, 0, 0);

    @Autowired
    private DiscordOutboxRepository discordOutboxRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("재시도 대상: PENDING이면서 next_attempt_at이 지난 행만, 오래된 순으로")
    void findsDuePendingOnly() {
        DiscordOutbox dueLater = em.persist(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 1L, NOW.minusMinutes(2)));
        DiscordOutbox dueEarlier = em.persist(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 2L, NOW.minusMinutes(5)));
        em.persist(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 3L, NOW));
        DiscordOutbox sent = DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 4L, NOW.minusMinutes(10));
        sent.markSent(NOW);
        em.persist(sent);
        em.flush();
        em.clear();

        List<DiscordOutbox> due = discordOutboxRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(DiscordOutboxStatus.PENDING, NOW);

        assertThat(due).extracting(DiscordOutbox::getId)
                .containsExactly(dueEarlier.getId(), dueLater.getId());
    }

    @Test
    @DisplayName("findByTypeAndTargetId: 종류와 대상으로 한 행을 찾는다")
    void findByTypeAndTargetId() {
        DiscordOutbox saved = em.persistAndFlush(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 7L, NOW));

        assertThat(discordOutboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, 7L))
                .get().extracting(DiscordOutbox::getId).isEqualTo(saved.getId());
        assertThat(discordOutboxRepository.findByTypeAndTargetId(DiscordOutboxType.TEST_CREATED, 8L)).isEmpty();
    }

    @Test
    @DisplayName("(type, target_id) 중복 저장은 막힌다")
    void uniqueTypeAndTarget() {
        discordOutboxRepository.saveAndFlush(DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 9L, NOW));

        assertThatThrownBy(() -> discordOutboxRepository.saveAndFlush(
                DiscordOutbox.pending(DiscordOutboxType.TEST_CREATED, 9L, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
