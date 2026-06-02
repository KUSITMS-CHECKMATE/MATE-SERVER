package server.MATE.domain.participation.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import server.MATE.domain.answer.dto.response.MyAnswerItemView;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class ParticipationQueryRepositoryImplTest {

    private static final Long TESTER_ID = 20L;
    private static final Long OTHER_TESTER_ID = 21L;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("existsActiveParticipation: 삭제되지 않은 참여가 있으면 true를 반환한다")
    void existsActiveParticipation_returnsTrue() {
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(TESTER_ID)
                .build());
        em.clear();

        boolean result = participationRepository.existsActiveParticipation(test.getId(), TESTER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsActiveParticipation: 삭제된 참여나 다른 testId testerId는 제외한다")
    void existsActiveParticipation_excludesDeletedOrDifferentParticipation() {
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        server.MATE.domain.test.entity.Test otherTest = persistTest("테스트 B");
        Participation deletedParticipation = em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(TESTER_ID)
                .build());
        em.persistAndFlush(Participation.builder()
                .testId(otherTest.getId())
                .testerId(TESTER_ID)
                .build());
        em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(OTHER_TESTER_ID)
                .build());
        markParticipationDeleted(deletedParticipation.getId(), LocalDateTime.now());
        em.clear();

        assertThat(participationRepository.existsActiveParticipation(test.getId(), TESTER_ID)).isFalse();
        assertThat(participationRepository.existsActiveParticipation(test.getId(), OTHER_TESTER_ID)).isTrue();
        assertThat(participationRepository.existsActiveParticipation(otherTest.getId(), TESTER_ID)).isTrue();
    }

    @Test
    @DisplayName("existsActiveParticipation: 필수 인자가 없으면 false를 반환한다")
    void existsActiveParticipation_missingRequiredArgs_returnsFalse() {
        assertThat(participationRepository.existsActiveParticipation(null, TESTER_ID)).isFalse();
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        assertThat(participationRepository.existsActiveParticipation(test.getId(), null)).isFalse();
    }

    @Test
    @DisplayName("findMyAnswerItemsByTesterId: createdAt 내림차순으로 projection을 반환한다")
    void findMyAnswerItemsByTesterId_returnsProjectionOrderedByCreatedAtDesc() {
        server.MATE.domain.test.entity.Test firstTest = persistTest("첫 번째 테스트");
        server.MATE.domain.test.entity.Test secondTest = persistTest("두 번째 테스트");

        Participation olderParticipation = em.persistAndFlush(Participation.builder()
                .testId(firstTest.getId())
                .testerId(TESTER_ID)
                .build());
        Participation newerParticipation = em.persistAndFlush(Participation.builder()
                .testId(secondTest.getId())
                .testerId(TESTER_ID)
                .build());

        updateParticipationCreatedAt(olderParticipation.getId(), LocalDateTime.of(2026, 5, 1, 10, 0));
        updateParticipationCreatedAt(newerParticipation.getId(), LocalDateTime.of(2026, 5, 2, 10, 0));
        em.clear();

        List<MyAnswerItemView> result = participationRepository.findMyAnswerItemsByTesterId(TESTER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MyAnswerItemView::testId).containsExactly(secondTest.getId(), firstTest.getId());
        assertThat(result).extracting(MyAnswerItemView::testName).containsExactly("두 번째 테스트", "첫 번째 테스트");
        assertThat(result).extracting(MyAnswerItemView::reward).containsExactly(300, 300);
    }

    @Test
    @DisplayName("findMyAnswerItemsByTesterId: 삭제된 participation과 삭제된 test는 제외한다")
    void findMyAnswerItemsByTesterId_excludesDeletedData() {
        server.MATE.domain.test.entity.Test activeTest = persistTest("활성 테스트");
        server.MATE.domain.test.entity.Test deletedTest = persistTest("삭제 테스트");

        Participation activeParticipation = em.persistAndFlush(Participation.builder()
                .testId(activeTest.getId())
                .testerId(TESTER_ID)
                .build());
        Participation deletedParticipation = em.persistAndFlush(Participation.builder()
                .testId(activeTest.getId())
                .testerId(OTHER_TESTER_ID)
                .build());
        Participation participationOnDeletedTest = em.persistAndFlush(Participation.builder()
                .testId(deletedTest.getId())
                .testerId(TESTER_ID)
                .build());

        updateParticipationCreatedAt(activeParticipation.getId(), LocalDateTime.of(2026, 5, 3, 10, 0));
        updateParticipationCreatedAt(deletedParticipation.getId(), LocalDateTime.of(2026, 5, 4, 10, 0));
        updateParticipationCreatedAt(participationOnDeletedTest.getId(), LocalDateTime.of(2026, 5, 5, 10, 0));
        markParticipationDeleted(deletedParticipation.getId(), LocalDateTime.now());
        markTestDeleted(deletedTest.getId(), LocalDateTime.now());
        em.clear();

        List<MyAnswerItemView> result = participationRepository.findMyAnswerItemsByTesterId(TESTER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().testId()).isEqualTo(activeTest.getId());
        assertThat(result.getFirst().testName()).isEqualTo("활성 테스트");
    }

    @Test
    @DisplayName("findMyAnswerItemsByTesterId: testerId가 없으면 빈 리스트를 반환한다")
    void findMyAnswerItemsByTesterId_missingTesterId_returnsEmpty() {
        assertThat(participationRepository.findMyAnswerItemsByTesterId(null)).isEmpty();
    }

    @Test
    @DisplayName("softDeleteByTestId: 해당 테스트의 삭제되지 않은 참여만 논리삭제한다")
    void softDeleteByTestId_updatesOnlyActiveParticipationsInTest() {
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        server.MATE.domain.test.entity.Test otherTest = persistTest("테스트 B");

        Participation activeParticipation1 = em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(TESTER_ID)
                .build());
        Participation activeParticipation2 = em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(OTHER_TESTER_ID)
                .build());
        Participation alreadyDeletedParticipation = em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(30L)
                .build());
        Participation otherTestParticipation = em.persistAndFlush(Participation.builder()
                .testId(otherTest.getId())
                .testerId(TESTER_ID)
                .build());

        LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 2, 12, 0);
        markParticipationDeleted(alreadyDeletedParticipation.getId(), LocalDateTime.of(2026, 6, 1, 12, 0));
        em.clear();

        long updatedCount = participationRepository.softDeleteByTestId(test.getId(), deletedAt);
        em.clear();

        assertThat(updatedCount).isEqualTo(2L);
        assertThat(findParticipationDeletedAt(activeParticipation1.getId())).isEqualTo(deletedAt);
        assertThat(findParticipationDeletedAt(activeParticipation2.getId())).isEqualTo(deletedAt);
        assertThat(findParticipationDeletedAt(alreadyDeletedParticipation.getId()))
                .isEqualTo(LocalDateTime.of(2026, 6, 1, 12, 0));
        assertThat(findParticipationDeletedAt(otherTestParticipation.getId())).isNull();
    }

    @Test
    @DisplayName("softDeleteByTestId: 필수 인자가 없으면 0을 반환한다")
    void softDeleteByTestId_missingRequiredArgs_returnsZero() {
        assertThat(participationRepository.softDeleteByTestId(null, LocalDateTime.now())).isZero();
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        assertThat(participationRepository.softDeleteByTestId(test.getId(), null)).isZero();
    }

    @Test
    @DisplayName("deleteByTestId: 해당 테스트의 참여만 모두 삭제한다")
    void deleteByTestId_deletesParticipationsInTest() {
        server.MATE.domain.test.entity.Test test = persistTest("테스트 A");
        server.MATE.domain.test.entity.Test otherTest = persistTest("테스트 B");
        em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(TESTER_ID)
                .build());
        em.persistAndFlush(Participation.builder()
                .testId(test.getId())
                .testerId(OTHER_TESTER_ID)
                .build());
        em.persistAndFlush(Participation.builder()
                .testId(otherTest.getId())
                .testerId(TESTER_ID)
                .build());
        em.clear();

        long deletedCount = participationRepository.deleteByTestId(test.getId());
        em.clear();

        assertThat(deletedCount).isEqualTo(2L);
        assertThat(countParticipationsByTestId(test.getId())).isZero();
        assertThat(countParticipationsByTestId(otherTest.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteByTestId: testId가 없으면 0을 반환한다")
    void deleteByTestId_missingTestId_returnsZero() {
        assertThat(participationRepository.deleteByTestId(null)).isZero();
    }

    private server.MATE.domain.test.entity.Test persistTest(String title) {
        return em.persistAndFlush(
                server.MATE.domain.test.entity.Test.builder()
                        .makerId(1L)
                        .title(title)
                        .description("설명")
                        .serviceName("서비스")
                        .serviceDescription("서비스 설명")
                        .goalPpl(10)
                        .reward(300)
                        .testStatus(TestStatus.IN_PROGRESS)
                        .closedAt(LocalDateTime.now().plusDays(7))
                        .build()
        );
    }

    private void updateParticipationCreatedAt(Long participationId, LocalDateTime createdAt) {
        em.getEntityManager()
                .createNativeQuery("update participation set created_at = ? where id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, participationId)
                .executeUpdate();
    }

    private void markParticipationDeleted(Long participationId, LocalDateTime deletedAt) {
        em.getEntityManager()
                .createQuery("update Participation p set p.deletedAt = :deletedAt where p.id = :id")
                .setParameter("deletedAt", deletedAt)
                .setParameter("id", participationId)
                .executeUpdate();
    }

    private void markTestDeleted(Long testId, LocalDateTime deletedAt) {
        em.getEntityManager()
                .createQuery("update Test t set t.deletedAt = :deletedAt where t.id = :id")
                .setParameter("deletedAt", deletedAt)
                .setParameter("id", testId)
                .executeUpdate();
    }

    private LocalDateTime findParticipationDeletedAt(Long participationId) {
        return em.getEntityManager()
                .createQuery("select p.deletedAt from Participation p where p.id = :id", LocalDateTime.class)
                .setParameter("id", participationId)
                .getSingleResult();
    }

    private long countParticipationsByTestId(Long testId) {
        Long count = em.getEntityManager()
                .createQuery("select count(p) from Participation p where p.testId = :testId", Long.class)
                .setParameter("testId", testId)
                .getSingleResult();
        return count == null ? 0L : count;
    }
}
