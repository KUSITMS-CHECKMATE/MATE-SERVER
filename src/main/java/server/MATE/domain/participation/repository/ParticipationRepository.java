package server.MATE.domain.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.participation.entity.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Long>, ParticipationQueryRepository {
}
