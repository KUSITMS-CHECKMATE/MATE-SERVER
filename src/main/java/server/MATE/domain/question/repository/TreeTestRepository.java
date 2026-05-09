package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.MATE.domain.question.entity.TreeTest;

import java.util.List;

public interface TreeTestRepository extends JpaRepository<TreeTest, Long> {

    @Query("""
            select node
            from TreeTest node
            left join fetch node.parent
            where node.question.id in :questionIds
            order by node.question.id asc, node.depth asc, node.sequence asc, node.id asc
            """)
    List<TreeTest> findAllByQuestionIdInOrderByQuestionAndTree(List<Long> questionIds);
}
