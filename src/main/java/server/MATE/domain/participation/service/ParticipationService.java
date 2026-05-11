package server.MATE.domain.participation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.participation.dto.response.ParticipationCreateResponse;
import server.MATE.domain.participation.entity.Participation;
import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.domain.test.entity.Test;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ParticipationService {

    private final TestRepository testRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public ParticipationCreateResponse createParticipation(Long testId, Long testerId) {
        Test test = testRepository.findByIdAndDeletedAtIsNull(testId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.TEST_004));

        test.validateCanParticipate();

        if (participationRepository.existsByTestIdAndTesterIdAndDeletedAtIsNull(testId, testerId)) {
            throw new BaseException(BaseErrorCode.PARTICIPATION_003);
        }

        Participation participation = Participation.builder()
                .testId(testId)
                .testerId(testerId)
                .build();

        participationRepository.save(participation);
        return ParticipationCreateResponse.from(participation);
    }
}
