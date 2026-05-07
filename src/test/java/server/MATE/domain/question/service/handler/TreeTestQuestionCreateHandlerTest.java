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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TreeTestQuestionCreateHandlerTest {

    @Mock
    private TreeTestRepository treeTestRepository;

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
}
