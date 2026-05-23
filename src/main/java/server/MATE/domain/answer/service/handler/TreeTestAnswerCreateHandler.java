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
    public void validate(AnswerCreateItem item, AnswerCreateContext context) {
        TreeTestAnswerCreateRequest request = (TreeTestAnswerCreateRequest) item;

        List<Long> path = request.path();

        // 요청한 마지막 nodeId는 path의 마지막 노드와 일치해야 함
        if (!request.nodeId().equals(path.getLast())) {
            throw new BaseException(BaseErrorCode.ANSWER_004);
        }

        List<TreeTest> nodes = context.treeNodes().get(request.questionId());
        if (nodes == null || nodes.isEmpty()) throw new BaseException(BaseErrorCode.QUESTION_005);

        Map<Long, TreeTest> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TreeTest::getId, n -> n));

        // path가 루트부터 부모-자식 관계를 올바르게 이어져야 함
        validatePath(path, nodeMap);

        TreeTest node = nodeMap.get(request.nodeId());
        boolean isLeaf = nodes.stream()
                .noneMatch(n -> n.getParent() != null && n.getParent().getId().equals(node.getId()));

        // 응답은 자식이 없는 leaf 노드만 최종 선택할 수 있음
        if (!isLeaf) throw new BaseException(BaseErrorCode.ANSWER_004);
    }

    @Override
    public Answer build(Long participationId, AnswerCreateItem item, AnswerCreateContext context) {
        TreeTestAnswerCreateRequest request = (TreeTestAnswerCreateRequest) item;
        List<Long> path = request.path();
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
