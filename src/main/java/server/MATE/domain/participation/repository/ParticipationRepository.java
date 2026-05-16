package server.MATE.domain.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.participation.entity.Participation;

import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsByTestIdAndTesterIdAndDeletedAtIsNull(Long testId, Long testerId);
}
