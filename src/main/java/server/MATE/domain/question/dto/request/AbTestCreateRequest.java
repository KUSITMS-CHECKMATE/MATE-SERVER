package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("AB_TEST")
public record AbTestCreateRequest(
        @Schema(example = "어느 시안이 더 마음에 드시나요?")
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Schema(example = "두 시안을 비교하고 더 선호하는 쪽을 선택해주세요.")
        @Size(max = 50, message = "질문 설명은 최대 50자까지 입력 가능합니다.")
        String description,

        @Schema(example = "ab-test-image-a")
        @NotBlank
        String aImageKey,

        @Schema(example = "ab-test-image-b")
        @NotBlank
        String bImageKey
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.AB_TEST;
    }
}
