package server.MATE.domain.question.dto.response;

import server.MATE.domain.question.dto.request.TreeTestCreateRequest;
import server.MATE.domain.question.entity.Question;

import java.util.List;

public record TreeTestCreateResponse(
        Long questionId,
        List<Feature> features
) {
    public record Feature(
            String label,
            List<TreeNode> children
    ) {}

    public record TreeNode(
            String label,
            List<TreeNode> children
    ) {}

    public static TreeTestCreateResponse from(Question question, TreeTestCreateRequest request) {
        return new TreeTestCreateResponse(
                question.getId(),
                request.features().stream().map(TreeTestCreateResponse::toFeature).toList()
        );
    }

    private static Feature toFeature(TreeTestCreateRequest.Feature f) {
        return new Feature(f.label(), toTreeNodes(f.children()));
    }

    private static List<TreeNode> toTreeNodes(List<TreeTestCreateRequest.TreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream()
                .map(n -> new TreeNode(n.label(), toTreeNodes(n.children())))
                .toList();
    }
}
