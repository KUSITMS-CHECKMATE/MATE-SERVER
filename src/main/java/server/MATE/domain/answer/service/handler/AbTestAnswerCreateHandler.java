package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AbTestAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.QuestionType;

import java.util.Map;

@Component
public class AbTestAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.AB_TEST;
    }

    @Override
    public void validate(AnswerCreateItem item, AnswerCreateContext context) {
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        AbTestAnswerCreateRequest request = (AbTestAnswerCreateRequest) item;

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.AB_TEST)
                .answer(Map.of("selected", request.selected()))
                .build();
    }
}
