package server.MATE.domain.question.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.MATE.domain.question.entity.TreeTest;

import java.util.List;
import java.util.Optional;

public interface TreeTestRepository extends JpaRepository<TreeTest, Long> {

    Optional<TreeTest> findByIdAndQuestion_Id(Long id, Long questionId);

    boolean existsByParent_Id(Long parentId);

    @Query("""
            select node
            from TreeTest node
            join fetch node.question
            left join fetch node.parent
            where node.question.id in :questionIds
            order by node.question.id asc, node.depth asc, node.sequence asc, node.id asc
            """)
    List<TreeTest> findAllByQuestionIdInOrderByQuestionAndTree(List<Long> questionIds);
}
