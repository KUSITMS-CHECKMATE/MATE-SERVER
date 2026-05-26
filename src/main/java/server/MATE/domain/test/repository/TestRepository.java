package server.MATE.domain.test.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    @EntityGraph(attributePaths = {"categories"})
    List<Test> findAllByTestStatusInAndDeletedAtIsNullAndClosedAtGreaterThanEqualOrderByCreatedAtDesc(
            List<TestStatus> testStatuses,
            LocalDateTime closedAt
    );

    List<Test> findAllByMakerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long makerId);

    @Query("""
            select t
            from Test t
            join TestLike tl on tl.testId = t.id
            where tl.userId = :userId
              and t.deletedAt is null
              and t.testStatus in :testStatuses
            order by tl.createdAt desc
            """)
    List<Test> findLikedTestsByUserId(
            @Param("userId") Long userId,
            @Param("testStatuses") List<TestStatus> testStatuses
    );

    Optional<Test> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"categories"})
    Optional<Test> findWithCategoriesByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Test t
            where t.id = :id
              and t.deletedAt is null
            """)
    Optional<Test> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
