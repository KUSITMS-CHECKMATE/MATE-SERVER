package server.MATE.domain.answer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.answer.entity.Answer;

import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findAllByQuestionIdInAndDeletedAtIsNull(List<Long> questionIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Answer a
            set a.deletedAt = :deletedAt
            where a.questionId in :questionIds
              and a.deletedAt is null
            """)
    int softDeleteAllByQuestionIds(@Param("questionIds") List<Long> questionIds,
                                   @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from Answer a
            where a.questionId in :questionIds
            """)
    int deleteAllByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
