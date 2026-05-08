package server.MATE.domain.test.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.MATE.domain.test.entity.Test;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

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
