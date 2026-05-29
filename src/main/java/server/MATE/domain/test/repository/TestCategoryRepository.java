package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.TestCategory;

import java.time.LocalDateTime;

public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TestCategory tc
            set tc.deletedAt = :deletedAt
            where tc.test.id = :testId
              and tc.deletedAt is null
            """)
    int softDeleteAllByTestId(@Param("testId") Long testId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from TestCategory tc
            where tc.test.id = :testId
            """)
    int deleteAllByTestId(@Param("testId") Long testId);
}
