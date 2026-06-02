package server.MATE.domain.test.repository;

import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.entity.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TestQueryRepository {

    List<Test> findAvailableTestsForUser(List<TestStatus> statuses, LocalDateTime closedAt, Long userId);

    List<Test> findByMakerId(Long makerId);

    List<Test> findLikedTests(Long userId, List<TestStatus> statuses);

    Optional<Test> findActiveById(Long id);

    Optional<Test> findByIdIncludingDeleted(Long id);

    long softDeleteById(Long id, LocalDateTime deletedAt);

    Optional<Test> findWithCategoriesById(Long id);

    Optional<Test> findByIdForUpdate(Long id);

    List<Test> findExpiredInProgressTests(LocalDateTime now);
}
