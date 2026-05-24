package server.MATE.domain.testdraft.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;

import java.util.List;
import java.util.Optional;

public interface TestDraftRepository extends JpaRepository<TestDraft, Long> {

    Optional<TestDraft> findById(Long id);

    List<TestDraft> findAllByMakerIdOrderByUpdatedAtDesc(Long makerId);

    List<TestDraft> findAllByStatus(TestDraftStatus status);

    Optional<TestDraft> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select td
            from TestDraft td
            where td.id = :id
            """)
    Optional<TestDraft> findByIdForUpdate(@Param("id") Long id);
}
