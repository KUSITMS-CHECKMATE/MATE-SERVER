package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.FiveSecond;

import java.util.List;

public interface FiveSecondRepository extends JpaRepository<FiveSecond, Long> {

    @EntityGraph(attributePaths = "options")
    List<FiveSecond> findAllByIdIn(Iterable<Long> ids);
}
