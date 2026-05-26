package server.MATE.domain.testdraft.dto.response;

import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.entity.TestDraftStatus;

import java.time.LocalDateTime;

public record MyTestDraftItem(
        Long draftId,
        String title,
        TestDraftStatus status,
        Integer goalPpl,
        Integer reward,
        LocalDateTime closedAt,
        LocalDateTime updatedAt
) {
    public static MyTestDraftItem from(TestDraft draft) {
        return new MyTestDraftItem(
                draft.getId(),
                draft.getTitle(),
                draft.getStatus(),
                draft.getGoalPpl(),
                draft.getReward(),
                draft.getClosedAt(),
                draft.getUpdatedAt()
        );
    }
}
