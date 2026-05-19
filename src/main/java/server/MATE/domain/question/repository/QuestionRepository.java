package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT COALESCE(MAX(q.sequence), 0) FROM Question q WHERE q.testId = :testId")
    Long findMaxSequenceByTestId(Long testId);

    Optional<Question> findByIdAndTestIdAndDeletedAtIsNull(Long id, Long testId);

    List<Question> findAllByTestIdAndDeletedAtIsNullOrderBySequenceAsc(Long testId);

    @Query("""
            select new server.MATE.domain.question.dto.response.QuestionSummaryItem(
                q.id,
                q.sequence,
                q.title,
                q.questionType
            )
            from Question q
            where q.testId = :testId
              and q.deletedAt is null
            order by q.sequence asc
            """)
    List<QuestionSummaryItem> findQuestionSummariesByTestId(Long testId);
}
