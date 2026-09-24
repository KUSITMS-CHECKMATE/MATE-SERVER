package server.MATE.domain.test.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class TestRepositoryWaitingSummaryTest {

    private static final LocalDateTime THRESHOLD = LocalDateTime.of(2026, 9, 24, 6, 0, 0);

    @Autowired
    private TestRepository testRepository;
    @Autowired
    private TestEntityManager em;

    private server.MATE.domain.test.entity.Test persist(String title, TestStatus status, LocalDateTime createdAt, boolean deleted) {
        server.MATE.domain.test.entity.Test test = em.persistAndFlush(server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title(title)
                .testStatus(status)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build());
        // createdAt은 auditing이 채우므로 저장 후 네이티브 업데이트로 원하는 시각 지정
        em.getEntityManager().createNativeQuery("update test set created_at = ?1, deleted_at = ?2 where id = ?3")
                .setParameter(1, createdAt)
                .setParameter(2, deleted ? createdAt : null)
                .setParameter(3, test.getId())
                .executeUpdate();
        return test;
    }

    @Test
    @DisplayName("WAITING·미삭제·기준 시각 이전만, 오래된 순 5건과 전체 건수")
    void findsLongWaitingTests() {
        for (int i = 0; i < 6; i++) {
            persist("old" + i, TestStatus.WAITING, THRESHOLD.minusHours(6 - i), false);
        }
        persist("recent", TestStatus.WAITING, THRESHOLD.plusMinutes(1), false);
        persist("approved", TestStatus.IN_PROGRESS, THRESHOLD.minusHours(10), false);
        persist("deleted", TestStatus.WAITING, THRESHOLD.minusHours(10), true);
        em.clear();

        List<server.MATE.domain.test.entity.Test> top = testRepository
                .findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(TestStatus.WAITING, THRESHOLD);
        long count = testRepository
                .countByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(TestStatus.WAITING, THRESHOLD);

        assertThat(top).extracting(server.MATE.domain.test.entity.Test::getTitle)
                .containsExactly("old0", "old1", "old2", "old3", "old4");
        assertThat(count).isEqualTo(6);
    }
}
