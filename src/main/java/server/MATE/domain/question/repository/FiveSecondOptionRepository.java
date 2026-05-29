package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.question.entity.FiveSecondOption;

import java.util.List;

public interface FiveSecondOptionRepository extends JpaRepository<FiveSecondOption, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from FiveSecondOption fo
            where fo.fiveSecond.id in :fiveSecondIds
            """)
    int deleteAllByFiveSecondIds(@Param("fiveSecondIds") List<Long> fiveSecondIds);
}
