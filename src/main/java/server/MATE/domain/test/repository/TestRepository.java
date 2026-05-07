package server.MATE.domain.test.repository;

<<<<<<< HEAD
import org.springframework.data.jpa.repository.EntityGraph;
=======
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
>>>>>>> origin/dev
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;

import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

<<<<<<< HEAD
    /** 카테고리까지 함께 로드 (open-in-view false 환경에서 상세 응답용). */
    @EntityGraph(attributePaths = "categories")
    @Query("SELECT t FROM Test t WHERE t.id = :id")
    Optional<Test> findWithCategoriesById(@Param("id") Long id);
=======
    Optional<Test> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Test t
            where t.id = :id
              and t.deletedAt is null
            """)
    Optional<Test> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
>>>>>>> origin/dev
}
