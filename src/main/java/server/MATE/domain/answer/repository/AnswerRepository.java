package server.MATE.domain.answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.answer.entity.Answer;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findAllByQuestionIdInAndDeletedAtIsNull(List<Long> questionIds);
}
