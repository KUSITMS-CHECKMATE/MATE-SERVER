package server.MATE.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.question.entity.question;

public interface questionRepository extends JpaRepository<question, Long> {
}
