package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.ScaleAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

@Component
public class ScaleAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.SCALE;
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        ScaleAnswerCreateRequest request = (ScaleAnswerCreateRequest) item;

        Scale scale = context.scales().get(request.questionId());
        if (scale == null) throw new BaseException(BaseErrorCode.QUESTION_005);

        if (request.value() < 1 || request.value() > scale.getRange()) {
            throw new BaseException(BaseErrorCode.ANSWER_007);
        }

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SCALE)
                .answer(Map.of("value", request.value()))
                .build();
    }
}
