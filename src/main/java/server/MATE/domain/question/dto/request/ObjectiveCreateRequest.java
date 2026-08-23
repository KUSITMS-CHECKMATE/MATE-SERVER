package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

import java.util.List;

@JsonTypeName("OBJECTIVE")
public record ObjectiveCreateRequest(
        @Schema(example = "가장 자주 사용하는 기능은 무엇인가요?")
        @NotBlank
        String title,

        @Schema(example = "해당 서비스를 사용할 때 가장 자주 쓰는 기능을 골라주세요.")
        String description,

        @Schema(example = "false")
        @NotNull(message = "중복 선택 여부는 필수 입력 사항입니다.")
        Boolean isDuplicate,

        @Schema(example = "2")
        Integer maxSelect,
        @Schema(example = "1")
        Integer minSelect,

        @Schema(example = "true")
        @NotNull(message = "기타 선택지 여부는 필수 입력 사항입니다.")
        Boolean isOther,

        @Schema(description = "객관식 선택지 목록")
        @NotNull(message = "선택지는 필수 입력 사항입니다.")
        @Size(min = 2, message = "선택지는 최소 2개 이상 추가해야 합니다.")
        List<@Valid ObjectiveOptionRequest> options
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.OBJECTIVE;
    }
}
