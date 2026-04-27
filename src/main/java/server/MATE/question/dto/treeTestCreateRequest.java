package server.MATE.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record treeTestCreateRequest(
        @NotNull(message = "testId는 필수입니다.")
        Long testId,
        @NotBlank(message = "title은 필수입니다.")
        String title,
        String description,
        Long sequence,
        @NotNull(message = "branches는 필수입니다.")
        @Size(min = 1, max = 4, message = "기능은 최소 1개, 최대 4개까지 입력할 수 있습니다.")
        List<@Valid Branch> branches
) {
        public record Branch(
                @NotBlank(message = "branch는 비어 있을 수 없습니다.")
                String branch,
                @NotNull(message = "branch_detail은 필수입니다.")
                @Size(min = 1, max = 4, message = "각 기능의 하위 항목은 최소 1개, 최대 4개까지 입력할 수 있습니다.")
                List<@NotBlank(message = "하위 기능 이름은 비어 있을 수 없습니다.") String> branch_detail
        ) {
        }
}
