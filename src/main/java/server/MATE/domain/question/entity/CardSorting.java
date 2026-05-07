package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 카드소팅 문항의 카드 목록·그룹 라벨을 JSON 컬럼에 보관 */
@Getter
@Entity
@Table(name = "card_sorting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSorting {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cards", nullable = false, columnDefinition = "json")
    private List<String> cards = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category", nullable = false, columnDefinition = "json")
    private List<CategoryItem> category = new ArrayList<>();

    private CardSorting(Question question, List<String> cards, List<CategoryItem> category) {
        this.question = question;
        this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards);
        this.category = new ArrayList<>();
        if (category != null) {
            category.stream().map(CategoryItem::copy).forEach(this.category::add);
        }
    }

    public static CardSorting create(Question question, List<String> cards, List<CategoryItem> category) {
        return new CardSorting(question, cards, category);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CategoryItem implements Serializable {
        private String name;

        public CategoryItem(String name) {
            this.name = name;
        }

        CategoryItem copy() {
            return new CategoryItem(this.name);
        }
    }
}
