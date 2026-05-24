package server.MATE.domain.test.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.ApprovalStatus;
import server.MATE.domain.test.entity.Test;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    @EntityGraph(attributePaths = {"categories"})
    List<Test> findAllByApprovalStatusAndDeletedAtIsNullOrderByCreatedAtDesc(ApprovalStatus approvalStatus);

    @EntityGraph(attributePaths = {"categories"})
    List<Test> findAllByMakerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long makerId);

    @Query("""
            select t
            from Test t
            join TestLike tl on tl.testId = t.id
            where tl.userId = :userId
              and t.deletedAt is null
              and t.approvalStatus = :approvalStatus
            order by tl.createdAt desc
            """)
    @EntityGraph(attributePaths = {"categories"})
    List<Test> findLikedTestsByUserId(
            @Param("userId") Long userId,
            @Param("approvalStatus") ApprovalStatus approvalStatus
    );

    Optional<Test> findByIdAndDeletedAtIsNull(Long id);

    Optional<Test> findByIdAndApprovalStatusAndDeletedAtIsNull(Long id, ApprovalStatus approvalStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Test t
            where t.id = :id
              and t.deletedAt is null
            """)
    Optional<Test> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
