package server.MATE.domain.answer.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnswerCreateRequest(
        @NotEmpty(message = "answers는 최소 1개 이상이어야 합니다.")
        List<@Valid AnswerCreateItem> answers
) {}
