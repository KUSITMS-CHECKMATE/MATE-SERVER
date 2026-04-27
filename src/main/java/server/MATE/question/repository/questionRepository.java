package server.MATE.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.question.entity.Question;

public interface questionRepository extends JpaRepository<Question, Long> {
}
