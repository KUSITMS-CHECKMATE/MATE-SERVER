package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.QuestionType;

import java.util.Map;

@Component
public class SubjectiveAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public void validate(AnswerCreateItem item, AnswerCreateContext context) {
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        SubjectiveAnswerCreateRequest request = (SubjectiveAnswerCreateRequest) item;
        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", request.text()))
                .build();
    }
}
