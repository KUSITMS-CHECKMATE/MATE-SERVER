package server.MATE.domain.question.dto.response;

import java.util.List;

public record TreeTestNodeDetailResponse(
        Long treeTestId,
        String label,
        List<TreeTestNodeDetailResponse> children
) {
}
