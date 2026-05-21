package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.report.dto.response.TreeTestNodeResult;
import server.MATE.domain.report.dto.response.TreeTestReportResult;
import server.MATE.domain.report.service.ReportHandler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TreeTestReportHandler implements ReportHandler {

    private final TreeTestRepository treeTestRepository;

    @Override
    public QuestionType supports() {
        return QuestionType.TREE_TEST;
    }

    @Override
    public Map<Long, Object> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, Map<Long, TreeTest>> nodesByQuestion = treeTestRepository
                .findAllByQuestionIdInOrderByQuestionAndTree(questionIds).stream()
                .collect(Collectors.groupingBy(
                        n -> n.getQuestion().getId(),
                        Collectors.toMap(TreeTest::getId, n -> n)
                ));

        Map<Long, Object> result = new LinkedHashMap<>();
        for (Question question : questions) {
            Map<Long, TreeTest> nodeMap = nodesByQuestion.getOrDefault(question.getId(), Map.of());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForTreeTest(nodeMap, answers));
        }
        return result;
    }

    private TreeTestReportResult computeForTreeTest(Map<Long, TreeTest> nodeMap, List<Answer> answers) {
        // 부모로 참조되는 노드 ID 집합 → 해당 ID는 리프가 아님
        Set<Long> parentIds = nodeMap.values().stream()
                .filter(n -> n.getParent() != null)
                .map(n -> n.getParent().getId())
                .collect(Collectors.toSet());

        // 리프 노드 전체를 0건으로 초기화 (선택받지 못한 노드도 결과에 포함)
        Map<Long, Integer> countByNodeId = new LinkedHashMap<>();
        nodeMap.values().stream()
                .filter(n -> !parentIds.contains(n.getId()))
                .forEach(n -> countByNodeId.put(n.getId(), 0));

        for (Answer answer : answers) {
            Object nodeIdObj = answer.getAnswer().get("nodeId");
            if (nodeIdObj == null) continue;
            Long nodeId = ((Number) nodeIdObj).longValue();
            countByNodeId.merge(nodeId, 1, Integer::sum);
        }

        int total = answers.size();
        List<TreeTestNodeResult> nodes = countByNodeId.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Long, Integer> e) -> e.getValue()).reversed())
                .map(e -> {
                    TreeTest node = nodeMap.get(e.getKey());
                    String label = node != null ? node.getLabel() : String.valueOf(e.getKey());
                    return new TreeTestNodeResult(e.getKey(), label, e.getValue(), ReportHandlerUtils.toPercentage(e.getValue(), total));
                })
                .toList();

        return new TreeTestReportResult(nodes);
    }
}
