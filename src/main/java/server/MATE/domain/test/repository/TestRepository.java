package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    @EntityGraph(attributePaths = {"categories", "imageKeys"})
    List<Test> findByDeletedAtIsNull(Sort sort);

    /** 카테고리/이미지까지 함께 로드 (open-in-view false 환경에서 상세 응답용). */
    @EntityGraph(attributePaths = {"categories", "imageKeys"})
    @Query("SELECT t FROM Test t WHERE t.id = :id AND t.deletedAt IS NULL")
    Optional<Test> findWithCategoriesById(@Param("id") Long id);

    Optional<Test> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Test t
            where t.id = :id
              and t.deletedAt is null
            """)
    Optional<Test> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
