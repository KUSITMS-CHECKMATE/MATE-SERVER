package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("AB_TEST")
public record AbTestCreateRequest(
        @Schema(example = "어느 시안이 더 마음에 드시나요?")
        @NotBlank
        String title,

        @Schema(example = "두 시안을 비교하고 더 선호하는 쪽을 선택해주세요.")
        String description,

        @Schema(example = "ab-test-image-a")
        @NotBlank
        String aImageKey,

        @Schema(example = "ab-test-image-b")
        @NotBlank
        String bImageKey,

        @Schema(example = "9:16", allowableValues = {"9:16", "1:1", "4:3"})
        @NotNull(message = "사진 비율은 필수 입력 사항입니다.")
        ImageRatio imageRatio
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.AB_TEST;
    }
}
