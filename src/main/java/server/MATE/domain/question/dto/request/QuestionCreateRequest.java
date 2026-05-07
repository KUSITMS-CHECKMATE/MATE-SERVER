package server.MATE.domain.question.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuestionCreateRequest(
        @Schema(
                description = "등록할 질문 문항 목록",
                example = """
                        [
                          {
                            "type": "OBJECTIVE",
                            "title": "가장 자주 사용하는 기능은 무엇인가요?",
                            "description": "해당 서비스를 사용할 때 가장 자주 쓰는 기능을 골라주세요.",
                            "isDuplicate": false,
                            "isOther": true,
                            "options": [
                              { "content": "검색", "imageKey": null },
                              { "content": "결제", "imageKey": "objective-option-image-key" }
                            ]
                          }
                        ]
                        """
        )
        @NotEmpty(message = "questions는 최소 1개 이상이어야 합니다.")
        List<@Valid QuestionCreateItem> questions
) {
}
