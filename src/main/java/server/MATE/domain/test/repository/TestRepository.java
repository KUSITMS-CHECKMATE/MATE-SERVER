package server.MATE.domain.test.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;

import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    /** 카테고리까지 함께 로드 (open-in-view false 환경에서 상세 응답용). */
    @EntityGraph(attributePaths = "categories")
    @Query("SELECT t FROM Test t WHERE t.id = :id")
    Optional<Test> findWithCategoriesById(@Param("id") Long id);
}
