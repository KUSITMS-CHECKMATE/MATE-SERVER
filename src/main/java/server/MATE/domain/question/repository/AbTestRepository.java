package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.AbTest;

public interface AbTestRepository extends JpaRepository<AbTest, Long> {
}
