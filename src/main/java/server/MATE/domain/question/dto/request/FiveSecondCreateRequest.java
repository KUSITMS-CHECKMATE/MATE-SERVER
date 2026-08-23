package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.ImageRatio;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("FIVE_SECOND")
public record FiveSecondCreateRequest(
        @Schema(example = "첫 화면에서 눈에 띄는 요소는 무엇인가요?")
        @NotBlank
        String title,

        @Schema(example = "이미지를 5초간 본 뒤 답변해주세요.")
        String description,

        @Schema(example = "five-second-image-key")
        @NotBlank(message = "이미지는 필수 입력 사항입니다.")
        String imageKey,

        @Schema(example = "9:16", allowableValues = {"9:16", "1:1", "4:3"})
        @NotNull(message = "사진 비율은 필수 입력 사항입니다.")
        ImageRatio imageRatio,

        @Schema(example = "true")
        @NotNull(message = "객관식 전환 여부는 필수 입력 사항입니다.")
        Boolean isObjective,

        @Schema(
                example = "false",
                nullable = true,
                description = "객관식일 때만 사용합니다. 주관식일 때는 null이어야 합니다."
        )
        Boolean isDuplicate,
        @Schema(
                example = "1",
                nullable = true,
                description = "중복 선택 객관식일 때만 사용합니다. 주관식이거나 단일 선택 객관식일 때는 null이어야 합니다."
        )
        Integer minSelect,
        @Schema(
                example = "2",
                nullable = true,
                description = "중복 선택 객관식일 때만 사용합니다. 주관식이거나 단일 선택 객관식일 때는 null이어야 합니다."
        )
        Integer maxSelect,

        @Schema(
                example = "true",
                nullable = true,
                description = "객관식일 때만 필수입니다. 주관식일 때는 null이어야 합니다."
        )
        Boolean isOther,

        @Schema(description = "5초 테스트 객관식 선택지", nullable = true)
        @Size(max = 10, message = "선택지는 최대 10개까지 추가 가능합니다.")
        List<@Valid FiveSecondOptionRequest> options
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.FIVE_SECOND;
    }
}
