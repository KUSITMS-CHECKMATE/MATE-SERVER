package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.question.entity.Objective;

import java.util.List;
import java.util.Optional;

public interface ObjectiveRepository extends JpaRepository<Objective, Long> {

    @EntityGraph(attributePaths = "options")
    List<Objective> findAllByIdIn(Iterable<Long> ids);

    @EntityGraph(attributePaths = "options")
    @Query("SELECT o FROM Objective o WHERE o.id = :id")
    Optional<Objective> findWithOptionsById(@Param("id") Long id);
}
