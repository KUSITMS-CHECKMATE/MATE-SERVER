package server.MATE.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.test.entity.Test;

public interface testRepository extends JpaRepository<Test, Long> {
}
