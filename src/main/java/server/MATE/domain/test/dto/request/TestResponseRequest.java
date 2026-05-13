package server.MATE.domain.test.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import server.MATE.domain.answer.dto.request.AnswerCreateItem;

import java.util.List;

public record TestResponseRequest(
        @NotEmpty List<@Valid AnswerCreateItem> answers
) {}
