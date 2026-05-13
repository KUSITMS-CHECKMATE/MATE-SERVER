package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubjectiveAnswerCreateRequest.class, name = "SUBJECTIVE"),
        @JsonSubTypes.Type(value = ObjectiveAnswerCreateRequest.class, name = "OBJECTIVE"),
        @JsonSubTypes.Type(value = FiveSecondAnswerCreateRequest.class, name = "FIVE_SECOND"),
        @JsonSubTypes.Type(value = ScaleAnswerCreateRequest.class, name = "SCALE"),
        @JsonSubTypes.Type(value = AbTestAnswerCreateRequest.class, name = "AB_TEST")
})
public sealed interface AnswerCreateItem permits
        SubjectiveAnswerCreateRequest,
        ObjectiveAnswerCreateRequest,
        FiveSecondAnswerCreateRequest,
        ScaleAnswerCreateRequest,
        AbTestAnswerCreateRequest {

    Long questionId();

    QuestionType type();
}
