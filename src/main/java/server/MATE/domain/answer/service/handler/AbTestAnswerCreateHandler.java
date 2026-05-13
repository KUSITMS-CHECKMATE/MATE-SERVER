package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AbTestAnswerCreateRequest;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AbTestAnswerCreateHandler implements AnswerCreateHandler {

    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.AB_TEST;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        AbTestAnswerCreateRequest request = (AbTestAnswerCreateRequest) item;

        if (request.selected() == null) throw new BaseException(BaseErrorCode.ANSWER_005);
        if (!request.selected().equals("A") && !request.selected().equals("B")) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.AB_TEST)
                .answer(Map.of("selected", request.selected()))
                .build();
        return answerRepository.save(answer);
    }
}
