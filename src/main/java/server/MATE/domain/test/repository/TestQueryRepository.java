package server.MATE.domain.test.repository;

import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TestQueryRepository {

    List<Test> findActiveTests(List<TestStatus> statuses, LocalDateTime closedAt);

    List<Test> findByMakerId(Long makerId);

    List<Test> findLikedTests(Long userId, List<TestStatus> statuses);

    Optional<Test> findActiveById(Long id);

    Optional<Test> findWithCategoriesById(Long id);

    Optional<Test> findByIdForUpdate(Long id);
}
