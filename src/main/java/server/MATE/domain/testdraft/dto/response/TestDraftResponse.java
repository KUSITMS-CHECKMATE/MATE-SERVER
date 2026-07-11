package server.MATE.domain.testdraft.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TestDraftResponse(
        Long draftId,
        Long makerId,
        String title,
        String description,
        String serviceName,
        String serviceDescription,
        List<String> imageKeys,
        List<String> categories,
        Integer goalPpl,
        Integer reward,
        LocalDateTime closedAt,
        JsonNode questionsPayload,
        TestDraftStatus status,
        Long publishedTestId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        PaymentAmountResponse amountBreakdown
) {
    public static TestDraftResponse from(TestDraft draft, JsonNode questionsPayload, PaymentAmountResponse amountBreakdown) {
        return new TestDraftResponse(
                draft.getId(),
                draft.getMakerId(),
                draft.getTitle(),
                draft.getDescription(),
                draft.getServiceName(),
                draft.getServiceDescription(),
                draft.getImageKeys(),
                draft.getCategories(),
                draft.getGoalPpl(),
                draft.getReward(),
                draft.getClosedAt(),
                questionsPayload,
                draft.getStatus(),
                draft.getPublishedTestId(),
                draft.getCreatedAt(),
                draft.getUpdatedAt(),
                amountBreakdown
        );
    }
}
