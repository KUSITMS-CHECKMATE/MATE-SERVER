package server.MATE.question.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "branch", nullable = false, columnDefinition = "json")
    private JsonNode branch;

    private TreeTest(Question question, JsonNode branch) {
        this.question = question;
        this.branch = branch;
    }

    public static TreeTest create(Question question, JsonNode branch) {
        return new TreeTest(question, branch);
    }
}
