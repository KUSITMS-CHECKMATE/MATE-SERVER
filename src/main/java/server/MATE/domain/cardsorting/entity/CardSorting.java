package server.MATE.domain.cardsorting.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import server.MATE.domain.question.entity.Question;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSorting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cards", nullable = false, columnDefinition = "json")
    @Size(min = 4, max = 10, message = "카드는 최소 4개, 최대 10개까지 저장할 수 있습니다.")
    private List<@NotBlank(message = "카드 값은 비어 있을 수 없습니다.") String> cards = new ArrayList<>();

    private CardSorting(Question question, List<String> cards) {
        this.question = question;
        this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards);
    }

    public static CardSorting create(Question question, List<String> cards) {
        return new CardSorting(question, cards);
    }
}
