package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TreeTestQuestionCreateHandlerTest {

    private static final int MAX_DEPTH = 4;
    private static final int MAX_BRANCHING = 4;

    @Mock
    private TreeTestRepository treeTestRepository;

    @Test
    @DisplayName("루트 기능이 1개인 트리테스트는 정상 생성된다")
    void createsTreeTestWithSingleRoot() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(new TreeTestCreateRequest.Feature("루트1", List.of()))
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.validate(request);
        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository).save(captor.capture());
        TreeTest root = captor.getValue();

        assertThat(root.getLabel()).isEqualTo("루트1");
        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.getSequence()).isEqualTo(1);
    }

    @Test
    @DisplayName("루트 기능이 4개인 트리테스트는 정상 생성된다")
    void createsTreeTestWithFourRoots() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(
                        new TreeTestCreateRequest.Feature("루트1", List.of()),
                        new TreeTestCreateRequest.Feature("루트2", List.of()),
                        new TreeTestCreateRequest.Feature("루트3", List.of()),
                        new TreeTestCreateRequest.Feature("루트4", List.of())
                )
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.validate(request);
        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(TreeTest::getSequence)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("트리테스트 상세 엔티티를 재귀적으로 생성하고 루트 노드를 저장한다")
    void createsTreeTestRecursivelyAndSavesRootNodes() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(
                        new TreeTestCreateRequest.Feature(
                                "루트1",
                                List.of(
                                        new TreeTestCreateRequest.TreeNode(
                                                "자식1",
                                                List.of(new TreeTestCreateRequest.TreeNode("손자1", List.of()))
                                        )
                                )
                        ),
                        new TreeTestCreateRequest.Feature("루트2", List.of())
                )
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository, times(2)).save(captor.capture());
        List<TreeTest> roots = captor.getAllValues();

        assertThat(roots).hasSize(2);
        assertThat(roots.get(0).getQuestion()).isEqualTo(question);
        assertThat(roots.get(0).getLabel()).isEqualTo("루트1");
        assertThat(roots.get(0).getDepth()).isEqualTo(0);
        assertThat(roots.get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getLabel()).isEqualTo("자식1");
        assertThat(roots.get(0).getChildren().get(0).getDepth()).isEqualTo(1);
        assertThat(roots.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getChildren().get(0).getLabel()).isEqualTo("손자1");
        assertThat(roots.get(0).getChildren().get(0).getChildren().get(0).getDepth()).isEqualTo(2);
        assertThat(roots.get(1).getLabel()).isEqualTo("루트2");
        assertThat(handler.extractImageKeys(request)).isEmpty();
    }

    @Test
    @DisplayName("깊이 4단계를 초과하는 트리테스트는 QUESTION_006 예외가 발생한다")
    void throwsQuestion006WhenTreeDepthExceedsFourLevels() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(new TreeTestCreateRequest.Feature(
                        "루트",
                        List.of(new TreeTestCreateRequest.TreeNode(
                                "1단계",
                                List.of(new TreeTestCreateRequest.TreeNode(
                                        "2단계",
                                        List.of(new TreeTestCreateRequest.TreeNode(
                                                "3단계",
                                                List.of(new TreeTestCreateRequest.TreeNode(
                                                        "4단계 초과",
                                                        List.of()
                                                ))
                                        ))
                                ))
                        ))
                ))
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.QUESTION_006);
        verifyNoInteractions(treeTestRepository);
    }

    @Test
    @DisplayName("루트 기능이 5개면 COMMON_002 예외가 발생한다")
    void throwsCommon002WhenRootFeatureCountExceedsFour() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(
                        new TreeTestCreateRequest.Feature("루트1", List.of()),
                        new TreeTestCreateRequest.Feature("루트2", List.of()),
                        new TreeTestCreateRequest.Feature("루트3", List.of()),
                        new TreeTestCreateRequest.Feature("루트4", List.of()),
                        new TreeTestCreateRequest.Feature("루트5", List.of())
                )
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
        verifyNoInteractions(treeTestRepository);
    }

    @Test
    @DisplayName("각 부모의 하위 항목이 5개면 COMMON_002 예외가 발생한다")
    void throwsCommon002WhenChildCountExceedsFourPerParent() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(new TreeTestCreateRequest.Feature(
                        "루트1",
                        List.of(
                                new TreeTestCreateRequest.TreeNode("자식1", List.of()),
                                new TreeTestCreateRequest.TreeNode("자식2", List.of()),
                                new TreeTestCreateRequest.TreeNode("자식3", List.of()),
                                new TreeTestCreateRequest.TreeNode("자식4", List.of()),
                                new TreeTestCreateRequest.TreeNode("자식5", List.of())
                        )
                ))
        );

        assertThatThrownBy(() -> handler.validate(request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
        verifyNoInteractions(treeTestRepository);
    }

    @Test
    @DisplayName("children이 null이어도 트리테스트를 정상 생성한다")
    void createsTreeTestWhenChildrenIsNull() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(new TreeTestCreateRequest.Feature("루트1", null))
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.validate(request);
        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository).save(captor.capture());
        assertThat(captor.getValue().getChildren()).isEmpty();
    }

    @Test
    @DisplayName("children이 빈 배열이어도 트리테스트를 정상 생성한다")
    void createsTreeTestWhenChildrenIsEmptyList() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "트리테스트",
                "설명",
                List.of(new TreeTestCreateRequest.Feature("루트1", List.of()))
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.validate(request);
        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository).save(captor.capture());
        assertThat(captor.getValue().getChildren()).isEmpty();
    }

    @Test
    @DisplayName("최대 340개 노드의 복잡한 트리테스트를 재귀적으로 생성한다")
    void createsComplexTreeWithMaximum340Nodes() {
        TreeTestQuestionCreateHandler handler = new TreeTestQuestionCreateHandler(treeTestRepository);
        TreeTestCreateRequest request = new TreeTestCreateRequest(
                "복잡한 트리테스트",
                "설명",
                createFullFeatures()
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.TREE_TEST)
                .title("복잡한 트리테스트")
                .description("설명")
                .sequence(1L)
                .build();

        handler.validate(request);
        handler.createDetail(question, request);

        ArgumentCaptor<TreeTest> captor = ArgumentCaptor.forClass(TreeTest.class);
        verify(treeTestRepository, times(MAX_BRANCHING)).save(captor.capture());
        List<TreeTest> roots = captor.getAllValues();

        assertThat(roots).hasSize(MAX_BRANCHING);
        assertThat(countNodes(roots)).isEqualTo(340);
        assertThat(maxDepth(roots)).isEqualTo(3);
        assertThat(allDepths(roots)).containsExactlyInAnyOrder(0, 1, 2, 3);
        assertThat(roots)
                .extracting(TreeTest::getSequence)
                .containsExactly(1, 2, 3, 4);
        assertThat(roots.get(0).getChildren())
                .extracting(TreeTest::getSequence)
                .containsExactly(1, 2, 3, 4);
        assertThat(roots.get(0).getQuestion()).isEqualTo(question);
    }

    private List<TreeTestCreateRequest.Feature> createFullFeatures() {
        return List.of(
                createFeature("루트1"),
                createFeature("루트2"),
                createFeature("루트3"),
                createFeature("루트4")
        );
    }

    private TreeTestCreateRequest.Feature createFeature(String label) {
        return new TreeTestCreateRequest.Feature(label, createChildren(2, label));
    }

    private List<TreeTestCreateRequest.TreeNode> createChildren(int currentLevel, String path) {
        if (currentLevel > MAX_DEPTH) {
            return List.of();
        }

        return List.of(
                createNode(currentLevel, path, 1),
                createNode(currentLevel, path, 2),
                createNode(currentLevel, path, 3),
                createNode(currentLevel, path, 4)
        );
    }

    private TreeTestCreateRequest.TreeNode createNode(int currentLevel, String path, int index) {
        String label = path + "-" + currentLevel + "-" + index;
        return new TreeTestCreateRequest.TreeNode(label, createChildren(currentLevel + 1, label));
    }

    private int countNodes(List<TreeTest> roots) {
        int count = 0;
        ArrayDeque<TreeTest> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            TreeTest node = queue.removeFirst();
            count += 1;
            queue.addAll(node.getChildren());
        }
        return count;
    }

    private int maxDepth(List<TreeTest> roots) {
        int maxDepth = 0;
        ArrayDeque<TreeTest> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            TreeTest node = queue.removeFirst();
            maxDepth = Math.max(maxDepth, node.getDepth());
            queue.addAll(node.getChildren());
        }
        return maxDepth;
    }

    private List<Integer> allDepths(List<TreeTest> roots) {
        List<Integer> depths = new ArrayList<>();
        ArrayDeque<TreeTest> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            TreeTest node = queue.removeFirst();
            depths.add(node.getDepth());
            queue.addAll(node.getChildren());
        }
        return depths.stream().distinct().toList();
    }
}
