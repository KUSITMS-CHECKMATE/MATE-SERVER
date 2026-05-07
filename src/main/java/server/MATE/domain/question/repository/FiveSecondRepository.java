package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.FiveSecond;

public interface FiveSecondRepository extends JpaRepository<FiveSecond, Long> {
}
