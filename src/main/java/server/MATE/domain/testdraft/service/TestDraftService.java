package server.MATE.domain.testdraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.payment.policy.IapProductTierCatalog;
import server.MATE.domain.testdraft.dto.request.TestDraftClosedAtParser;
import server.MATE.domain.testdraft.dto.request.TestDraftUpdateRequest;
import server.MATE.domain.testdraft.dto.response.MyTestDraftItem;
import server.MATE.domain.testdraft.dto.response.MyTestDraftResponse;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TestDraftService {

    private final TestDraftRepository testDraftRepository;
    private final ObjectMapper objectMapper;
    private final IapProductTierCatalog iapProductTierCatalog;
    private final TestDraftValidator testDraftValidator;

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
                parseClosedAt(request.closedAt()),
                toMap(request.questionsPayload())
        );
        if (draft.getGoalPpl() != null && draft.getReward() != null) {
            validateTierExists(draft.getGoalPpl(), draft.getReward());
        }
        testDraftRepository.saveAndFlush(draft);
        return TestDraftResponse.from(draft, toJsonNode(draft.getQuestionsPayload()));
    }

    public void deleteDraft(Long draftId, Long makerId) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        testDraftRepository.delete(draft);
    }

    @Transactional(readOnly = true)
    public void publishCheck(Long draftId, Long makerId) {
        TestDraft draft = getOwnedDraft(draftId, makerId);
        draft.validatePublishState();
        draft.validateAmountFields();
        validateTierExists(draft.getGoalPpl(), draft.getReward());
        testDraftValidator.validateForPublish(draft);
    }

    private void validateTierExists(Integer goalPpl, Integer reward) {
        iapProductTierCatalog.find(goalPpl, reward)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_007));
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

    private LocalDateTime parseClosedAt(String closedAt) {
        if (closedAt == null || closedAt.isBlank()) {
            return null;
        }
        return TestDraftClosedAtParser.parse(closedAt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode payload) {
        return payload == null || !payload.isObject()
                ? null
                : objectMapper.convertValue(payload, Map.class);
    }
}
