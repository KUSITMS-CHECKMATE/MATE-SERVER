package server.MATE.domain.test.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;
import server.MATE.domain.test.repository.TestLikeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class TestQueryRepositoryImplTest {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestLikeRepository testLikeRepository;

    @Autowired
    private TestEntityManager em;

    private server.MATE.domain.test.entity.Test savedTest;

    @BeforeEach
    void setUp() {
        savedTest = em.persistAndFlush(
                server.MATE.domain.test.entity.Test.builder()
                        .makerId(1L)
                        .title("테스트 제목")
                        .testStatus(TestStatus.IN_PROGRESS)
                        .goalPpl(10)
                        .reward(300)
                        .closedAt(LocalDateTime.now().plusDays(7))
                        .build()
        );
        em.clear();
    }

    @Test
    @DisplayName("findActiveById: 삭제되지 않은 테스트를 id로 조회한다")
    void findActiveById_returnsTest() {
        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findActiveById(savedTest.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedTest.getId());
    }

    @Test
    @DisplayName("findActiveById: deletedAt이 설정된 테스트는 조회하지 않는다")
    void findActiveById_deletedTest_returnsEmpty() {
        em.getEntityManager()
                .createQuery("update Test t set t.deletedAt = :now where t.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", savedTest.getId())
                .executeUpdate();
        em.clear();

        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findActiveById(savedTest.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findWithCategoriesById: 카테고리 없는 테스트도 정상 조회된다")
    void findWithCategoriesById_noCategories_returnsTest() {
        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findWithCategoriesById(savedTest.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCategories()).isEmpty();
    }

    @Test
    @DisplayName("findWithCategoriesById: 카테고리가 있는 테스트를 조회하면 categories가 fetchJoin으로 로드된다")
    void findWithCategoriesById_withCategories_returnsCategoriesLoaded() {
        server.MATE.domain.test.entity.Test managed =
                em.getEntityManager().find(server.MATE.domain.test.entity.Test.class, savedTest.getId());
        managed.addCategories(List.of(Category.DAILY, Category.FINANCE));
        em.flush();
        em.clear();

        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findWithCategoriesById(savedTest.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCategories()).hasSize(2);
        assertThat(result.get().getCategories())
                .extracting(tc -> tc.getCategory())
                .containsExactlyInAnyOrder(Category.DAILY, Category.FINANCE);
    }

    @Test
    @DisplayName("findWithCategoriesById: 삭제된 테스트는 조회하지 않는다")
    void findWithCategoriesById_deletedTest_returnsEmpty() {
        em.getEntityManager()
                .createQuery("update Test t set t.deletedAt = :now where t.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", savedTest.getId())
                .executeUpdate();
        em.clear();

        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findWithCategoriesById(savedTest.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdForUpdate: 테스트를 비관적 락으로 조회한다")
    void findByIdForUpdate_returnsTest() {
        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findByIdForUpdate(savedTest.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedTest.getId());
    }

    @Test
    @DisplayName("findByIdForUpdate: deletedAt이 설정된 테스트는 조회하지 않는다")
    void findByIdForUpdate_deletedTest_returnsEmpty() {
        em.getEntityManager()
                .createQuery("update Test t set t.deletedAt = :now where t.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", savedTest.getId())
                .executeUpdate();
        em.clear();

        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findByIdForUpdate(savedTest.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdForUpdate: 존재하지 않는 id면 빈 Optional을 반환한다")
    void findByIdForUpdate_notFound_returnsEmpty() {
        Optional<server.MATE.domain.test.entity.Test> result =
                testRepository.findByIdForUpdate(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAvailableTestsForUser: 주어진 상태와 마감일 조건에 맞는 테스트를 반환한다")
    void findAvailableTestsForUser_returnsMatchingTests() {
        List<server.MATE.domain.test.entity.Test> result = testRepository.findAvailableTestsForUser(
                List.of(TestStatus.IN_PROGRESS),
                LocalDateTime.now(),
                10L
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(savedTest.getId());
    }

    @Test
    @DisplayName("findAvailableTestsForUser: 마감된 테스트는 결과에 포함하지 않는다")
    void findAvailableTestsForUser_excludesExpiredTests() {
        em.getEntityManager()
                .createQuery("update Test t set t.closedAt = :past where t.id = :id")
                .setParameter("past", LocalDateTime.now().minusDays(1))
                .setParameter("id", savedTest.getId())
                .executeUpdate();
        em.clear();

        List<server.MATE.domain.test.entity.Test> result = testRepository.findAvailableTestsForUser(
                List.of(TestStatus.IN_PROGRESS),
                LocalDateTime.now(),
                10L
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAvailableTestsForUser: 필수 인자가 없으면 빈 리스트를 반환한다")
    void findAvailableTestsForUser_missingRequiredArgs_returnsEmpty() {
        assertThat(testRepository.findAvailableTestsForUser(null, LocalDateTime.now(), 10L)).isEmpty();
        assertThat(testRepository.findAvailableTestsForUser(List.of(), LocalDateTime.now(), 10L)).isEmpty();
        assertThat(testRepository.findAvailableTestsForUser(List.of(TestStatus.IN_PROGRESS), null, 10L)).isEmpty();
        assertThat(testRepository.findAvailableTestsForUser(List.of(TestStatus.IN_PROGRESS), LocalDateTime.now(), null)).isEmpty();
    }

    @Test
    @DisplayName("findAvailableTestsForUser: 삭제된 테스트는 결과에 포함하지 않는다")
    void findAvailableTestsForUser_excludesDeletedTests() {
        em.getEntityManager()
                .createQuery("update Test t set t.deletedAt = :now where t.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", savedTest.getId())
                .executeUpdate();
        em.clear();

        List<server.MATE.domain.test.entity.Test> result = testRepository.findAvailableTestsForUser(
                List.of(TestStatus.IN_PROGRESS),
                LocalDateTime.now(),
                10L
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAvailableTestsForUser: 이미 응답한 테스트는 결과에 포함하지 않는다")
    void findAvailableTestsForUser_excludesParticipatedTests() {
        em.persistAndFlush(Participation.builder()
                .testId(savedTest.getId())
                .testerId(10L)
                .build());
        em.clear();

        List<server.MATE.domain.test.entity.Test> result = testRepository.findAvailableTestsForUser(
                List.of(TestStatus.IN_PROGRESS),
                LocalDateTime.now(),
                10L
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByMakerId: 특정 제작자의 삭제되지 않은 테스트 목록을 반환한다")
    void findByMakerId_returnsTestsByMaker() {
        em.persistAndFlush(
                server.MATE.domain.test.entity.Test.builder()
                        .makerId(1L)
                        .title("두 번째 테스트")
                        .testStatus(TestStatus.WAITING)
                        .goalPpl(10)
                        .reward(300)
                        .closedAt(LocalDateTime.now().plusDays(7))
                        .build()
        );
        em.clear();

        List<server.MATE.domain.test.entity.Test> result =
                testRepository.findByMakerId(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("makerId").containsOnly(1L);
    }

    @Test
    @DisplayName("findByMakerId: 다른 제작자의 테스트는 포함하지 않는다")
    void findByMakerId_excludesOtherMakers() {
        List<server.MATE.domain.test.entity.Test> result =
                testRepository.findByMakerId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByMakerId: makerId가 null이면 빈 리스트를 반환한다")
    void findByMakerId_nullMakerId_returnsEmpty() {
        assertThat(testRepository.findByMakerId(null)).isEmpty();
    }

    @Test
    @DisplayName("findLikedTests: 유저가 좋아요한 테스트 목록을 반환한다")
    void findLikedTests_returnsLikedTests() {
        testLikeRepository.save(
                server.MATE.domain.test.entity.TestLike.builder()
                        .userId(10L)
                        .testId(savedTest.getId())
                        .build()
        );
        em.flush();
        em.clear();

        List<server.MATE.domain.test.entity.Test> result = testRepository.findLikedTests(
                10L,
                List.of(TestStatus.IN_PROGRESS)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(savedTest.getId());
    }

    @Test
    @DisplayName("findLikedTests: 좋아요하지 않은 테스트는 포함하지 않는다")
    void findLikedTests_excludesNotLiked() {
        List<server.MATE.domain.test.entity.Test> result = testRepository.findLikedTests(
                10L,
                List.of(TestStatus.IN_PROGRESS)
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLikedTests: 필수 인자가 없으면 빈 리스트를 반환한다")
    void findLikedTests_missingRequiredArgs_returnsEmpty() {
        assertThat(testRepository.findLikedTests(null, List.of(TestStatus.IN_PROGRESS))).isEmpty();
        assertThat(testRepository.findLikedTests(10L, null)).isEmpty();
        assertThat(testRepository.findLikedTests(10L, List.of())).isEmpty();
    }

    @Test
    @DisplayName("findActiveById: id가 null이면 빈 Optional을 반환한다")
    void findActiveById_nullId_returnsEmpty() {
        assertThat(testRepository.findActiveById(null)).isEmpty();
    }

    @Test
    @DisplayName("findWithCategoriesById: id가 null이면 빈 Optional을 반환한다")
    void findWithCategoriesById_nullId_returnsEmpty() {
        assertThat(testRepository.findWithCategoriesById(null)).isEmpty();
    }

    @Test
    @DisplayName("findByIdForUpdate: id가 null이면 빈 Optional을 반환한다")
    void findByIdForUpdate_nullId_returnsEmpty() {
        assertThat(testRepository.findByIdForUpdate(null)).isEmpty();
    }
}
