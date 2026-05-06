package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Scale;

public interface ScaleRepository extends JpaRepository<Scale, Long> {
}
