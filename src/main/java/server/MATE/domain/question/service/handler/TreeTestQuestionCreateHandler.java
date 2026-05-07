package server.MATE.domain.question.service.handler;

import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;

import java.util.List;

@Component
public class TreeTestQuestionCreateHandler extends AbstractQuestionCreateHandler<TreeTestCreateRequest> {

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

    @Override
    protected List<String> extractImageKeysTyped(TreeTestCreateRequest item) {
        return List.of();
    }
}
