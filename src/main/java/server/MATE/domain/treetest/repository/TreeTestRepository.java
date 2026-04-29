package server.MATE.domain.treetest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.MATE.domain.treetest.entity.TreeTest;

public interface TreeTestRepository extends JpaRepository<TreeTest, Long> {
}
