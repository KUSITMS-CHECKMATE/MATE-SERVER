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
        JsonNode questionsPayload,
        TestDraftStatus status,
        Long publishedTestId,
        String orderNo,
        Integer expectedAmount,
        String payToken,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TestDraftResponse from(TestDraft draft, JsonNode questionsPayload) {
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
                questionsPayload,
                draft.getStatus(),
                draft.getPublishedTestId(),
                draft.getOrderNo(),
                draft.getExpectedAmount(),
                draft.getPayToken(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
