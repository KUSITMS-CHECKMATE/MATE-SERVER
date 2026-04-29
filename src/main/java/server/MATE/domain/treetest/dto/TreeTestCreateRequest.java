package server.MATE.domain.treetest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TreeTestCreateRequest(
        @NotNull(message = "testId는 필수입니다.")
        Long testId,
        @NotBlank(message = "title은 필수입니다.")
        String title,
        String description,
        Long sequence,
        @NotNull(message = "features는 필수입니다.")
        @Size(min = 1, max = 4, message = "기능은 최소 1개, 최대 4개까지 입력할 수 있습니다.")
        List<@Valid Feature> features
) {
        public record Feature(
                @NotBlank(message = "기능 이름은 비어 있을 수 없습니다.")
                String label,
                @Size(max = 4, message = "하위 항목은 최대 4개까지 입력할 수 있습니다.")
                List<@Valid TreeNode> children
        ) {}

        public record TreeNode(
                @NotBlank(message = "노드 이름은 비어 있을 수 없습니다.")
                String label,
                @Size(max = 4, message = "하위 항목은 최대 4개까지 입력할 수 있습니다.")
                List<@Valid TreeNode> children
        ) {}
}
