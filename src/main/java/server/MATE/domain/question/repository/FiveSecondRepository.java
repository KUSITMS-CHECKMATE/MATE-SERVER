package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.question.entity.FiveSecond;

import java.util.List;
import java.util.Optional;

public interface FiveSecondRepository extends JpaRepository<FiveSecond, Long> {

    @EntityGraph(attributePaths = "options")
    List<FiveSecond> findAllByIdIn(Iterable<Long> ids);

    @EntityGraph(attributePaths = "options")
    @Query("SELECT f FROM FiveSecond f WHERE f.id = :id")
    Optional<FiveSecond> findWithOptionsById(@Param("id") Long id);

    @EntityGraph(attributePaths = "options")
    List<FiveSecond> findAllByQuestion_IdIn(Iterable<Long> questionIds);
}
