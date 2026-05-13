package server.MATE.domain.answer.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.TreeTestAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TreeTestAnswerCreateHandler implements AnswerCreateHandler {

    private final TreeTestRepository treeTestRepository;
    private final AnswerRepository answerRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    public Answer save(Long participationId, AnswerCreateItem item) {
        TreeTestAnswerCreateRequest request = (TreeTestAnswerCreateRequest) item;

        if (request.nodeId() == null) throw new BaseException(BaseErrorCode.ANSWER_005);

        TreeTest node = treeTestRepository.findByIdAndQuestion_Id(request.nodeId(), request.questionId())
                .orElseThrow(() -> new BaseException(BaseErrorCode.ANSWER_004));

        if (treeTestRepository.existsByParent_Id(node.getId())) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        Answer answer = Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.TREE_TEST)
                .answer(Map.of("nodeId", request.nodeId()))
                .build();
        return answerRepository.save(answer);
    }
}
