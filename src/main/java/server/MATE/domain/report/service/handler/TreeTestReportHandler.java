package server.MATE.domain.report.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.question.repository.TreeTestRepository;
import server.MATE.domain.report.service.ReportHandler;

import java.util.ArrayList;
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
    public Map<Long, Map<String, Object>> compute(List<Question> questions, Map<Long, List<Answer>> answersByQuestionId) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        Map<Long, Map<Long, TreeTest>> nodesByQuestion = treeTestRepository
                .findAllByQuestionIdInOrderByQuestionAndTree(questionIds).stream()
                .collect(Collectors.groupingBy(
                        n -> n.getQuestion().getId(),
                        Collectors.toMap(TreeTest::getId, n -> n)
                ));

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Question question : questions) {
            Map<Long, TreeTest> nodeMap = nodesByQuestion.getOrDefault(question.getId(), Map.of());
            List<Answer> answers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            result.put(question.getId(), computeForTreeTest(nodeMap, answers));
        }
        return result;
    }

    private Map<String, Object> computeForTreeTest(Map<Long, TreeTest> nodeMap, List<Answer> answers) {
        Set<Long> parentIds = nodeMap.values().stream()
                .filter(n -> n.getParent() != null)
                .map(n -> n.getParent().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> countByNodeId = new LinkedHashMap<>();
        nodeMap.values().stream()
                .filter(n -> !parentIds.contains(n.getId()))
                .forEach(n -> countByNodeId.put(n.getId(), 0));

        Map<List<Long>, Integer> pathCounts = new LinkedHashMap<>();

        for (Answer answer : answers) {
            Object nodeIdObj = answer.getAnswer().get("nodeId");
            if (nodeIdObj == null) continue;
            Long nodeId = ((Number) nodeIdObj).longValue();
            countByNodeId.merge(nodeId, 1, Integer::sum);

            List<Long> path = extractPath(answer.getAnswer());
            if (!path.isEmpty()) {
                pathCounts.merge(path, 1, Integer::sum);
            }
        }

        int total = answers.size();

        List<Map<String, Object>> nodeFrequency = countByNodeId.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<Long, Integer> e) -> e.getValue()).reversed())
                .map(e -> {
                    TreeTest node = nodeMap.get(e.getKey());
                    String label = node != null ? node.getLabel() : String.valueOf(e.getKey());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nodeId", e.getKey());
                    m.put("label", label);
                    m.put("count", e.getValue());
                    m.put("ratio", ReportHandlerUtils.toRatio(e.getValue(), total));
                    return m;
                })
                .toList();

        List<Map<String, Object>> pathFrequency = pathCounts.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<List<Long>, Integer> e) -> e.getValue()).reversed())
                .map(e -> {
                    List<Long> path = e.getKey();
                    List<String> pathLabels = path.stream()
                            .map(id -> nodeMap.containsKey(id) ? nodeMap.get(id).getLabel() : String.valueOf(id))
                            .toList();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("path", path);
                    m.put("pathLabels", pathLabels);
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeFrequency", nodeFrequency);
        result.put("pathFrequency", pathFrequency);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractPath(Map<String, Object> answerMap) {
        List<Object> raw = (List<Object>) answerMap.get("path");
        if (raw == null) return List.of();
        List<Long> path = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Number n) path.add(n.longValue());
        }
        return path;
    }
}
