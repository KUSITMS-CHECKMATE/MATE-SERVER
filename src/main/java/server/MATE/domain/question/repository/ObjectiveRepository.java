package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Objective;

import java.util.List;

public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    @EntityGraph(attributePaths = "options")
    List<Objective> findAllByIdIn(Iterable<Long> ids);
}
