package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.ScaleAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScaleAnswerCreateHandler implements AnswerCreateHandler {

    private final ScaleRepository scaleRepository;
    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.SCALE;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        ScaleAnswerCreateRequest request = (ScaleAnswerCreateRequest) item;

        if (request.value() == null) throw new BaseException(BaseErrorCode.ANSWER_005);

        Scale scale = scaleRepository.findById(request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        if (request.value() < 1 || request.value() > scale.getRange()) {
            throw new BaseException(BaseErrorCode.ANSWER_007);
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SCALE)
                .answer(Map.of("value", request.value()))
                .build();
        return answerRepository.save(answer);
    }
}
