package server.MATE.domain.treetest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.testRepository;
import server.MATE.domain.treetest.dto.TreeTestCreateRequest;
import server.MATE.domain.treetest.entity.TreeTest;
import server.MATE.domain.treetest.repository.TreeTestRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TreeTestService {

    private final QuestionRepository questionRepository;
    private final testRepository testRepository;
    private final TreeTestRepository treeTestRepository;

    @Transactional
    public void createTreeTest(TreeTestCreateRequest request) {
        Long testId = Objects.requireNonNull(request.testId());
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(ErrorCode.TEST_001));

        Question question = Question.createTreeTestQuestion(
                test, request.title(), request.description(), request.sequence()
        );
        questionRepository.save(question);

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
