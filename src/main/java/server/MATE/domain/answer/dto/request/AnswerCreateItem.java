package server.MATE.domain.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubjectiveAnswerCreateRequest.class, name = "SUBJECTIVE"),
        @JsonSubTypes.Type(value = ObjectiveAnswerCreateRequest.class, name = "OBJECTIVE"),
        @JsonSubTypes.Type(value = FiveSecondAnswerCreateRequest.class, name = "FIVE_SECOND")
})
public sealed interface AnswerCreateItem permits
        SubjectiveAnswerCreateRequest,
        ObjectiveAnswerCreateRequest,
        FiveSecondAnswerCreateRequest {

    Long questionId();

    QuestionType type();
}
