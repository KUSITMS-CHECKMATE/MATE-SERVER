package server.MATE.domain.question.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import server.MATE.domain.question.entity.QuestionType;

@JsonTypeName("SUBJECTIVE")
public record SubjectiveCreateRequest(
        @Schema(example = "개선이 필요한 점은 무엇인가요?")
        @NotBlank
        @Size(max = 34, message = "질문 제목은 최대 34자까지 입력 가능합니다.")
        String title,

        @Schema(example = "자유롭게 작성해주세요.")
        @Size(max = 55, message = "질문 설명은 최대 55자까지 입력 가능합니다.")
        String description,

        @Schema(example = "subjective-image-key", nullable = true)
        String imageKey
) implements QuestionCreateItem {
    @Override
    public QuestionType type() {
        return QuestionType.SUBJECTIVE;
    }
}
