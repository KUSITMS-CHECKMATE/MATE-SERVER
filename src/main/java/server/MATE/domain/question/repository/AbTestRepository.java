package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.question.entity.AbTest;

import java.util.List;

public interface AbTestRepository extends JpaRepository<AbTest, Long> {

    List<AbTest> findAllByIdIn(Iterable<Long> ids);

    List<AbTest> findAllByQuestion_IdIn(Iterable<Long> questionIds);
}
