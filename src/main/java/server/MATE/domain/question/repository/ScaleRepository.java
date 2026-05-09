package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.Scale;

import java.util.List;

public interface ScaleRepository extends JpaRepository<Scale, Long> {

    List<Scale> findAllByIdIn(Iterable<Long> ids);
}
