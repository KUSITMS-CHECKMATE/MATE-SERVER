package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.test.entity.Test;

public interface TestRepository extends JpaRepository<Test, Long>, TestQueryRepository {
}
