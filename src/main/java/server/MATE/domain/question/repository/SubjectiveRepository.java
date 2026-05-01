package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Subjective;

public interface SubjectiveRepository extends JpaRepository<Subjective, Long> {
}
