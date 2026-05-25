package server.MATE.domain.testdraft.dto.response;

import java.util.List;

public record MyTestDraftResponse(
        int draftCount,
        List<MyTestDraftItem> drafts
) {
    public static MyTestDraftResponse from(List<MyTestDraftItem> drafts) {
        return new MyTestDraftResponse(drafts.size(), drafts);
    }
}
