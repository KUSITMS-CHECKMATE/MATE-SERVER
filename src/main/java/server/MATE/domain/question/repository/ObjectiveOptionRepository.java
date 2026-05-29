package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.question.entity.ObjectiveOption;

import java.util.List;

public interface ObjectiveOptionRepository extends JpaRepository<ObjectiveOption, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from ObjectiveOption oo
            where oo.objective.id in :objectiveIds
            """)
    int deleteAllByObjectiveIds(@Param("objectiveIds") List<Long> objectiveIds);
}
