package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ObjectiveCreateRequest.class, name = "OBJECTIVE"),
        @JsonSubTypes.Type(value = SubjectiveCreateRequest.class, name = "SUBJECTIVE"),
        @JsonSubTypes.Type(value = FiveSecondCreateRequest.class, name = "FIVE_SECOND"),
        @JsonSubTypes.Type(value = ScaleCreateRequest.class, name = "SCALE"),
        @JsonSubTypes.Type(value = AbTestCreateRequest.class, name = "AB_TEST"),
        @JsonSubTypes.Type(value = CardSortingCreateRequest.class, name = "CARD_SORTING"),
        @JsonSubTypes.Type(value = TreeTestCreateRequest.class, name = "TREE_TEST")
})
public sealed interface QuestionCreateItem permits
        ObjectiveCreateRequest,
        SubjectiveCreateRequest,
        FiveSecondCreateRequest,
        ScaleCreateRequest,
        AbTestCreateRequest,
        CardSortingCreateRequest,
        TreeTestCreateRequest {

    QuestionType type();

    String title();

    String description();
}
