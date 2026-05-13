package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.SubjectiveAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.QuestionType;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubjectiveAnswerCreateHandler implements AnswerCreateHandler {

    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.SUBJECTIVE;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        SubjectiveAnswerCreateRequest request = (SubjectiveAnswerCreateRequest) item;
        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", request.text()))
                .build();
        return answerRepository.save(answer);
    }
}
