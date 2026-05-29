package server.MATE.domain.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.answer.dto.response.MyAnswerItemView;
import server.MATE.domain.participation.entity.Participation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByTestIdAndTesterIdAndDeletedAtIsNull(Long testId, Long testerId);

    List<Participation> findAllByTestIdAndDeletedAtIsNull(Long testId);

    @Query("""
            select new server.MATE.domain.answer.dto.response.MyAnswerItemView(
                t.id,
                t.title,
                p.createdAt,
                t.reward
            )
            from Participation p
            join Test t on t.id = p.testId
            where p.testerId = :testerId
              and p.deletedAt is null
              and t.deletedAt is null
            order by p.createdAt desc
            """)
    List<MyAnswerItemView> findMyAnswerItemsByTesterIdOrderByCreatedAtDesc(@Param("testerId") Long testerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Participation p
            set p.deletedAt = :deletedAt
            where p.testId = :testId
              and p.deletedAt is null
            """)
    int softDeleteAllByTestId(@Param("testId") Long testId, @Param("deletedAt") LocalDateTime deletedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from Participation p
            where p.testId = :testId
            """)
    int deleteAllByTestId(@Param("testId") Long testId);
}
