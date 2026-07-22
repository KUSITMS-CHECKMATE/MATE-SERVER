package server.MATE.domain.testdraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import server.MATE.domain.payment.policy.IapProductTierCatalog;
import server.MATE.domain.testdraft.dto.request.TestDraftUpdateRequest;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.domain.testdraft.validator.TestDraftValidator;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class TestDraftServiceUpdateIntegrationTest {

    @Autowired
    private TestDraftRepository testDraftRepository;

    @Autowired
    private TestEntityManager em;

    private TestDraftService testDraftService;

    private static final Long MAKER_ID = 1L;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        testDraftService = new TestDraftService(
                testDraftRepository,
                objectMapper,
                mock(IapProductTierCatalog.class),
                mock(TestDraftValidator.class)
        );
    }

    @Test
    @DisplayName("기본정보 수정 시 응답 updatedAt이 실제 DB 반영값과 일치한다")
    void updateDraft_basicInfo_updatedAtMatchesPersistedValue() {
        TestDraft draft = em.persistAndFlush(TestDraft.builder()
                .makerId(MAKER_ID)
                .build());
        Long draftId = draft.getId();
        LocalDateTime initialUpdatedAt = draft.getUpdatedAt();
        em.clear();

        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                "수정된 제목", null, null, null, null, null, null, null, null, null
        );
        TestDraftResponse response = testDraftService.updateDraft(draftId, MAKER_ID, request);

        em.flush();
        em.clear();
        TestDraft persisted = testDraftRepository.findById(draftId).orElseThrow();

        assertThat(response.updatedAt()).isEqualTo(persisted.getUpdatedAt());
        assertThat(response.updatedAt()).isNotEqualTo(initialUpdatedAt);
    }

    @Test
    @DisplayName("질문목록 수정 시 응답 updatedAt이 실제 DB 반영값과 일치한다")
    void updateDraft_questionsPayload_updatedAtMatchesPersistedValue() {
        TestDraft draft = em.persistAndFlush(TestDraft.builder()
                .makerId(MAKER_ID)
                .build());
        Long draftId = draft.getId();
        LocalDateTime initialUpdatedAt = draft.getUpdatedAt();
        em.clear();

        JsonNode questionsPayload = new ObjectMapper().valueToTree(
                Map.of("questions", List.of(
                        Map.of("type", "SUBJECTIVE", "title", "질문 제목", "description", "질문 설명")
                ))
        );
        TestDraftUpdateRequest request = new TestDraftUpdateRequest(
                null, null, null, null, null, null, null, null, null, questionsPayload
        );
        TestDraftResponse response = testDraftService.updateDraft(draftId, MAKER_ID, request);

        em.flush();
        em.clear();
        TestDraft persisted = testDraftRepository.findById(draftId).orElseThrow();

        assertThat(response.updatedAt()).isEqualTo(persisted.getUpdatedAt());
        assertThat(response.updatedAt()).isNotEqualTo(initialUpdatedAt);
    }
}
