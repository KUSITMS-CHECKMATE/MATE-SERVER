package server.MATE.domain.answer.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;
import server.MATE.domain.answer.dto.request.TreeTestAnswerCreateRequest;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.service.AnswerCreateContext;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

@Component
public class TreeTestAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        TreeTestAnswerCreateRequest request = (TreeTestAnswerCreateRequest) item;

        List<TreeTest> nodes = context.treeNodes().get(request.questionId());
        if (nodes == null || nodes.isEmpty()) throw new BaseException(BaseErrorCode.QUESTION_005);

        TreeTest node = nodes.stream()
                .filter(n -> n.getId().equals(request.nodeId()))
                .findFirst()
                .orElseThrow(() -> new BaseException(BaseErrorCode.ANSWER_004));

        boolean isLeaf = nodes.stream()
                .noneMatch(n -> n.getParent() != null && n.getParent().getId().equals(node.getId()));
        if (!isLeaf) throw new BaseException(BaseErrorCode.ANSWER_004);

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.TREE_TEST)
                .answer(Map.of("nodeId", request.nodeId()))
                .build();
    }
}
