package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Objective;

public interface ObjectiveRepository extends JpaRepository<Objective, Long> {
}
