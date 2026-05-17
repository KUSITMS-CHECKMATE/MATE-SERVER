package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.test.entity.TestLike;

import java.util.Optional;

public interface TestLikeRepository extends JpaRepository<TestLike, Long> {

    boolean existsByUserIdAndTestId(Long userId, Long testId);

    Optional<TestLike> findByUserIdAndTestId(Long userId, Long testId);
}
