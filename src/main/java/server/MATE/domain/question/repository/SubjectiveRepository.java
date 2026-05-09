package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Subjective;

import java.util.List;

public interface SubjectiveRepository extends JpaRepository<Subjective, Long> {

    List<Subjective> findAllByIdIn(Iterable<Long> ids);
}
