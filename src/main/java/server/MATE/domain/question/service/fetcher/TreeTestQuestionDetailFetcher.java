package server.MATE.domain.question.service.fetcher;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.question.dto.response.QuestionDetailItem;
import server.MATE.domain.question.dto.response.TreeTestNodeDetailResponse;
import server.MATE.domain.question.dto.response.TreeTestDetailResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TreeTestQuestionDetailFetcher implements QuestionDetailFetcher {

    private final TreeTestRepository treeTestRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    public Map<Long, QuestionDetailItem> fetch(List<Question> questions) {
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Long> questionIds = new ArrayList<>(questionMap.keySet());

        List<TreeTest> nodes = treeTestRepository.findAllByQuestionIdInOrderByQuestionAndTree(questionIds);
        Map<Long, List<TreeTest>> nodesByQuestionId = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.getQuestion().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<Long, QuestionDetailItem> result = new LinkedHashMap<>();
        for (Long questionId : questionIds) {
            List<TreeTestNodeDetailResponse> features = buildFeatures(nodesByQuestionId.getOrDefault(questionId, List.of()));
            result.put(questionId, TreeTestDetailResponse.of(questionMap.get(questionId), features));
        }
        return result;
    }

    private List<TreeTestNodeDetailResponse> buildFeatures(List<TreeTest> nodes) {
        Map<Long, List<TreeTest>> childrenByParentId = nodes.stream()
                .filter(node -> node.getParent() != null)
                .collect(Collectors.groupingBy(node -> node.getParent().getId(), LinkedHashMap::new, Collectors.toList()));

        return nodes.stream()
                .filter(node -> node.getParent() == null)
                .map(node -> toNode(node, childrenByParentId))
                .toList();
    }

    private TreeTestNodeDetailResponse toNode(TreeTest node, Map<Long, List<TreeTest>> childrenByParentId) {
        List<TreeTestNodeDetailResponse> children = childrenByParentId.getOrDefault(node.getId(), List.of()).stream()
                .map(child -> toNode(child, childrenByParentId))
                .toList();

        return new TreeTestNodeDetailResponse(node.getId(), node.getLabel(), children);
    }
}
