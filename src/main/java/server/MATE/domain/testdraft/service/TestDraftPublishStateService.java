package server.MATE.domain.testdraft.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.testdraft.entity.TestDraft;
import server.MATE.domain.testdraft.repository.TestDraftRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@RequiredArgsConstructor
public class TestDraftPublishStateService {

    private final TestDraftRepository testDraftRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishFailed(Long draftId) {
        TestDraft draft = testDraftRepository.findById(draftId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.DRAFT_001));
        draft.markPublishFailed();
    }
}
