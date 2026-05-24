package server.MATE.domain.testdraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.testdraft.dto.request.TestDraftUpdateRequest;
import server.MATE.domain.testdraft.dto.response.MyTestDraftItem;
import server.MATE.domain.testdraft.dto.response.MyTestDraftResponse;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TestDraftService {

    private final TestDraftRepository testDraftRepository;
    private final ObjectMapper objectMapper;

    public TestDraftResponse createDraft(Long makerId) {
        TestDraft draft = testDraftRepository.save(TestDraft.builder()
                .makerId(makerId)
                .build());
        return TestDraftResponse.from(draft, null);
    }

    @Transactional(readOnly = true)
    public TestDraftResponse getDraft(Long draftId, Long makerId) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        return TestDraftResponse.from(draft, toJsonNode(draft.getQuestionsPayload()));
    }

    @Transactional(readOnly = true)
    public MyTestDraftResponse listMyDrafts(Long makerId) {
        List<MyTestDraftItem> items = testDraftRepository.findAllByMakerIdOrderByUpdatedAtDesc(makerId)
                .stream()
                .map(MyTestDraftItem::from)
                .toList();
        return MyTestDraftResponse.from(items);
    }

    public TestDraftResponse updateDraft(Long draftId, Long makerId, TestDraftUpdateRequest request) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        draft.update(
                request.title(),
                request.description(),
                request.serviceName(),
                request.serviceDescription(),
                request.imageKeys(),
                request.categories() == null ? null : request.categories().stream().map(Enum::name).toList(),
                request.goalPpl(),
                request.reward(),
                toMap(request.questionsPayload())
        );
        return TestDraftResponse.from(draft, toJsonNode(draft.getQuestionsPayload()));
    }

    public void deleteDraft(Long draftId, Long makerId) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        testDraftRepository.delete(draft);
    }

    private TestDraft getOwnedDraft(Long draftId, Long makerId) {
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        if (!draft.getMakerId().equals(makerId)) {
            throw new BaseException(BaseErrorCode.DRAFT_002);
        }
        return draft;
    }

    private JsonNode toJsonNode(Map<String, Object> payload) {
        return payload == null ? null : objectMapper.valueToTree(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode payload) {
        return payload == null || !payload.isObject()
                ? null
                : objectMapper.convertValue(payload, Map.class);
    }
}
