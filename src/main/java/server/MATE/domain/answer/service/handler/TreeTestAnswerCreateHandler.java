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
import java.util.stream.Collectors;

@Component
public class TreeTestAnswerCreateHandler implements AnswerCreateHandler {

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        TreeTestAnswerCreateRequest request = (TreeTestAnswerCreateRequest) item;

        List<Long> path = request.path();

        if (!request.nodeId().equals(path.getLast())) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        List<TreeTest> nodes = context.treeNodes().get(request.questionId());
        if (nodes == null || nodes.isEmpty()) throw new BaseException(BaseErrorCode.QUESTION_005);

        Map<Long, TreeTest> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TreeTest::getId, n -> n));

        validatePath(path, nodeMap);

        TreeTest node = nodeMap.get(request.nodeId());
        boolean isLeaf = nodes.stream()
                .noneMatch(n -> n.getParent() != null && n.getParent().getId().equals(node.getId()));
        if (!isLeaf) throw new BaseException(BaseErrorCode.ANSWER_004);

        return Answer.builder()
                .participationId(participationId)
                .questionId(request.questionId())
                .questionType(QuestionType.TREE_TEST)
                .answer(Map.of("nodeId", request.nodeId(), "path", path))
                .build();
    }

    private void validatePath(List<Long> path, Map<Long, TreeTest> nodeMap) {
        for (int i = 0; i < path.size(); i++) {
            TreeTest current = nodeMap.get(path.get(i));
            if (current == null) throw new BaseException(BaseErrorCode.ANSWER_004);

            if (i == 0) {
                if (current.getParent() != null) throw new BaseException(BaseErrorCode.ANSWER_004);
            } else {
                if (current.getParent() == null || !current.getParent().getId().equals(path.get(i - 1))) {
                    throw new BaseException(BaseErrorCode.ANSWER_004);
                }
            }
        }
    }
}
