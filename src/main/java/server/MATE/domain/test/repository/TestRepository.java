package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long>, TestQueryRepository {

    List<Test> findByTestStatusAndDeletedAtIsNull(TestStatus testStatus);

    List<Test> findTop5ByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            TestStatus testStatus, LocalDateTime threshold);

    long countByTestStatusAndDeletedAtIsNullAndCreatedAtLessThanEqual(TestStatus testStatus, LocalDateTime threshold);
}
