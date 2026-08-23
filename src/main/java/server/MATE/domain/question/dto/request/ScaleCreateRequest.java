package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("SCALE")
public record ScaleCreateRequest(
        @Schema(example = "전반적인 만족도를 평가해주세요.")
        @NotBlank
        String title,

        @Schema(example = "5점 척도로 응답해주세요.")
        String description,

        @Schema(example = "scale-image-key", nullable = true)
        String imageKey,

        @Schema(example = "매우 불만족")
        @Size(max = 100, message = "최소 점수 라벨은 최대 100자까지 입력 가능합니다.")
        String minLabel,

        @Schema(example = "매우 만족")
        @Size(max = 100, message = "최대 점수 라벨은 최대 100자까지 입력 가능합니다.")
        String maxLabel,

        @Schema(example = "5", allowableValues = {"5", "7"})
        @NotNull(message = "척도 범위는 필수입니다. 5 또는 7을 입력해주세요.")
        Integer range
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.SCALE;
    }
}
