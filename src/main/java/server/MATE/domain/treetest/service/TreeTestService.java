package server.MATE.domain.treetest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.treetest.dto.TreeTestCreateRequest;
import server.MATE.domain.treetest.entity.TreeTest;
import server.MATE.domain.treetest.repository.TreeTestRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeTestService {

    private final QuestionRepository questionRepository;
    private final TreeTestRepository treeTestRepository;

    @Transactional
    public void createTreeTest(Long questionId, TreeTestCreateRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(ErrorCode.QUESTION_001));

        for (int i = 0; i < request.features().size(); i++) {
            TreeTestCreateRequest.Feature feature = request.features().get(i);
            TreeTest root = TreeTest.create(question, null, feature.label(), i + 1);
            buildChildren(root, feature.children(), question);
            treeTestRepository.save(root);
        }
    }

    private void buildChildren(TreeTest parent, List<TreeTestCreateRequest.TreeNode> children, Question question) {
        if (children == null || children.isEmpty()) return;
        for (int i = 0; i < children.size(); i++) {
            TreeTestCreateRequest.TreeNode node = children.get(i);
            TreeTest child = TreeTest.create(question, parent, node.label(), i + 1);
            buildChildren(child, node.children(), question);
        }
    }
}
