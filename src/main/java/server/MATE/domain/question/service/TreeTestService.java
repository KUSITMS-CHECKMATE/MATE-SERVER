package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.dto.response.TreeTestCreateResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TreeTestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TreeTestRepository treeTestRepository;

    @Transactional
    public TreeTestCreateResponse createTreeTest(Long testId, Long makerId, TreeTestCreateRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));
        if (!test.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.TEST_005);
        }

        // TODO: sequence 로직 변경 예정
        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.TREE_TEST)
                .title(request.title())
                .description(request.description())
                .sequence(null)
                .build();
        questionRepository.save(question);

        for (int i = 0; i < request.features().size(); i++) {
            TreeTestCreateRequest.Feature feature = request.features().get(i);
            TreeTest root = TreeTest.create(question, null, feature.label(), i + 1);
            buildChildren(root, feature.children(), question);
            treeTestRepository.save(root);
        }
        return TreeTestCreateResponse.from(question, request);
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
