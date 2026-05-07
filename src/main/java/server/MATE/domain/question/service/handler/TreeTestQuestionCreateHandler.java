package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Component
public class TreeTestQuestionCreateHandler extends AbstractQuestionCreateHandler<TreeTestCreateRequest> {

    private static final int MAX_TREE_DEPTH = 4;

    private final TreeTestRepository treeTestRepository;

    public TreeTestQuestionCreateHandler(TreeTestRepository treeTestRepository) {
        super(TreeTestCreateRequest.class);
        this.treeTestRepository = treeTestRepository;
    }

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    protected void validateTyped(TreeTestCreateRequest item) {
        // 루트는 최소 1개, 최대 4개까지 허용함
        if (item.features() == null || item.features().isEmpty() || item.features().size() > 4) {
            throw new BaseException(BaseErrorCode.COMMON_002);
        }

        for (TreeTestCreateRequest.Feature feature : item.features()) {
            // 자식 노드는 최대 4개까지 허용함
            if (feature.children() != null && feature.children().size() > 4) {
                throw new BaseException(BaseErrorCode.COMMON_002);
            }
            countAndValidate(feature.children(), 2);
        }
    }

    @Override
    protected void createDetailTyped(Question question, TreeTestCreateRequest item) {
        for (int i = 0; i < item.features().size(); i++) {
            TreeTestCreateRequest.Feature feature = item.features().get(i);
            TreeTest root = TreeTest.create(question, null, feature.label(), i + 1);
            buildChildren(root, feature.children(), question);
            treeTestRepository.save(root);
        }
    }

    private void buildChildren(TreeTest parent, List<TreeTestCreateRequest.TreeNode> children, Question question) {
        if (children == null || children.isEmpty()) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            TreeTestCreateRequest.TreeNode node = children.get(i);
            TreeTest child = TreeTest.create(question, parent, node.label(), i + 1);
            buildChildren(child, node.children(), question);
        }
    }

    private int countAndValidate(List<TreeTestCreateRequest.TreeNode> children, int currentDepth) {
        if (children == null || children.isEmpty()) {
            return 0;
        }

        // 트리 깊이는 루트 포함 최대 4단계까지 허용한다.
        if (currentDepth > MAX_TREE_DEPTH) {
            throw new BaseException(BaseErrorCode.QUESTION_006);
        }

        int count = 0;
        for (TreeTestCreateRequest.TreeNode child : children) {
            // 각 자식 노드도 자신의 하위 항목을 최대 4개까지만 가질 수 있다.
            if (child.children() != null && child.children().size() > 4) {
                throw new BaseException(BaseErrorCode.COMMON_002);
            }
            count += 1;
            count += countAndValidate(child.children(), currentDepth + 1);
        }
        return count;
    }

    @Override
    protected List<String> extractImageKeysTyped(TreeTestCreateRequest item) {
        return List.of();
    }
}
