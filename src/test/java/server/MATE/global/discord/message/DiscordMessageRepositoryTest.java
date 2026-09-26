package server.MATE.global.discord.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class DiscordMessageRepositoryTest {

    @Autowired
    private DiscordMessageRepository repository;

    @Test
    @DisplayName("종류와 대상 ID로 메시지 위치를 찾는다")
    void findByTypeAndTargetId() {
        repository.saveAndFlush(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L, "777", "1234567890123456789"));

        assertThat(repository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L))
                .get()
                .extracting(DiscordMessage::getChannelId, DiscordMessage::getMessageId)
                .containsExactly("777", "1234567890123456789");
        assertThat(repository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, 16L)).isEmpty();
    }

    @Test
    @DisplayName("같은 종류·대상으로 두 번 저장하면 유니크 제약 위반")
    void uniqueTypeAndTarget() {
        repository.saveAndFlush(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L, "777", "1"));

        assertThatThrownBy(() -> repository.saveAndFlush(
                DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L, "777", "2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("종류와 대상 ID로 삭제한다")
    void deleteByTypeAndTargetId() {
        repository.saveAndFlush(DiscordMessage.create(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L, "777", "1"));

        int deleted = repository.deleteByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findByTypeAndTargetId(DiscordMessageType.REPORT_AGGREGATION_FAILED, 15L)).isEmpty();
    }
}
