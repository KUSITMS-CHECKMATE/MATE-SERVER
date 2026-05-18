package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.TestLike;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestLikeRepository extends JpaRepository<TestLike, Long> {

    boolean existsByUserIdAndTestId(Long userId, Long testId);

    Optional<TestLike> findByUserIdAndTestId(Long userId, Long testId);

    @Query("""
            select tl.testId
            from TestLike tl
            where tl.userId = :userId
              and tl.testId in :testIds
            """)
    List<Long> findLikedTestIds(@Param("userId") Long userId, @Param("testIds") Collection<Long> testIds);
}
