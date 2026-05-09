package server.MATE.domain.question.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

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
<<<<<<< HEAD
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
=======
    @Size(min = 4, max = 12, message = "카드는 최소 4개, 최대 12개까지 저장할 수 있습니다.")
    private List<@NotBlank(message = "카드 값은 비어 있을 수 없습니다.") String> cards = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", nullable = false, columnDefinition = "json")
    @Size(max = 3, message = "카테고리는 최대 3개까지 저장할 수 있습니다.")
    private List<@NotBlank(message = "카테고리 값은 비어 있을 수 없습니다.") String> categories = new ArrayList<>();

    public CardSorting(Question question, List<String> cards, List<String> categories) {
        this.question = question;
        this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards);
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
    }

    public static CardSorting create(Question question, List<String> cards, List<String> categories) {
        return new CardSorting(question, cards, categories);
>>>>>>> origin/dev
    }
}
