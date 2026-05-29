package server.MATE.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.report.entity.Report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            select r
            from Report r
            where r.testId = :testId
              and r.deletedAt is null
            """)
    List<Report> findAllByTestId(@Param("testId") Long testId);

    @Query("""
            select r
            from Report r
            where r.testId = :testId
              and r.questionId = :questionId
              and r.deletedAt is null
            """)
    Optional<Report> findByTestIdAndQuestionId(@Param("testId") Long testId, @Param("questionId") Long questionId);

    @Query("""
            select (count(r) > 0)
            from Report r
            where r.testId = :testId
              and r.deletedAt is null
            """)
    boolean existsByTestId(@Param("testId") Long testId);

    @Query("""
            select count(r)
            from Report r
            where r.testId = :testId
              and r.deletedAt is null
            """)
    long countByTestId(@Param("testId") Long testId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Report r
            set r.deletedAt = :deletedAt
            where r.testId = :testId
              and r.deletedAt is null
            """)
    int softDeleteAllByTestId(@Param("testId") Long testId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from Report r
            where r.testId = :testId
            """)
    int deleteAllByTestId(@Param("testId") Long testId);
}
