package server.MATE.domain.question.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.MATE.domain.question.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT COALESCE(MAX(q.sequence), 0) FROM Question q WHERE q.testId = :testId")
    Long findMaxSequenceByTestId(Long testId);

    List<Question> findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(Long testId);
}
