package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import server.MATE.global.common.entity.BaseEntity;

@Getter
@Entity
@Table(name = "tree_test")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeTest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TreeTest parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<TreeTest> children = new ArrayList<>();

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Integer sequence;

    @Column(nullable = false)
    private Integer depth;

    public static TreeTest create(Question question, TreeTest parent, String label, Integer sequence) {
        TreeTest node = new TreeTest();
        node.question = question;
        node.parent = parent;
        node.label = label;
        node.sequence = sequence;
        node.depth = (parent == null) ? 0 : parent.depth + 1;
        if (parent != null) {
            parent.children.add(node);
        }
        return node;
    }
}
