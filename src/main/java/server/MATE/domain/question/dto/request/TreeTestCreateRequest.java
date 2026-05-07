package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("TREE_TEST")
public record TreeTestCreateRequest(
        @Schema(example = "설정 메뉴에서 알림 설정을 어디서 찾으시겠어요?")
        @NotBlank(message = "질문 제목은 필수입니다.")
        String title,
        @Schema(example = "예상되는 경로를 따라 선택해주세요.")
        String description,
        @Schema(description = "트리테스트 루트 기능 목록")
        @NotNull(message = "features는 필수입니다.")
        @Size(min = 1, max = 4, message = "기능은 최소 1개, 최대 4개까지 입력할 수 있습니다.")
        List<@Valid Feature> features
) implements QuestionCreateItem {
        @Override
        public QuestionType type() {
                return QuestionType.TREE_TEST;
        }

        public record Feature(
                @Schema(example = "마이페이지")
                @NotBlank(message = "기능 이름은 비어 있을 수 없습니다.")
                String label,
                @Schema(description = "하위 노드 목록")
                @Size(max = 4, message = "하위 항목은 최대 4개까지 입력할 수 있습니다.")
                List<@Valid TreeNode> children
        ) {}

        public record TreeNode(
                @Schema(example = "설정")
                @NotBlank(message = "노드 이름은 비어 있을 수 없습니다.")
                String label,
                @Schema(description = "다음 단계 하위 노드 목록")
                @Size(max = 4, message = "하위 항목은 최대 4개까지 입력할 수 있습니다.")
                List<@Valid TreeNode> children
        ) {}
}
