package server.MATE.domain.answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.answer.entity.Answer;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    boolean existsByParticipationIdAndQuestionIdAndDeletedAtIsNull(Long participationId, Long questionId);
}
