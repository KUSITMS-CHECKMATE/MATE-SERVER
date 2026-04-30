package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.question.dto.request.SubjectiveQuestionCreateRequest;
import server.MATE.domain.question.dto.response.SubjectiveQuestionCreateResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Subjective;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.SubjectiveRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.image.event.ImageCleanupEvent;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubjectiveQuestionService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final SubjectiveRepository subjectiveRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SubjectiveQuestionCreateResponse createSubjectiveQuestion(
            Long testId,
            SubjectiveQuestionCreateRequest request
    ) {
        if (!testRepository.existsById(testId)) {
            throw new BaseException(ErrorCode.TEST_004);
        }

        // TODO: 동시성 이슈 - 현재 MAX(sequence)+1 방식은 동시 요청 시 중복 순서값 발생 가능.
        //  별도 시퀀스 관리 로직 개발 후 수정 예정.
        Long sequence = questionRepository.findMaxSequenceByTestId(testId) + 1;

        Question question = Question.builder()
                .testId(testId)
                .questionType(QuestionType.SUBJECTIVE)
                .title(request.title())
                .description(request.description())
                .sequence(sequence)
                .build();

        Subjective subjective = Subjective.builder()
                .question(question)
                .imageKey(request.imageKey())
                .build();

        if (request.imageKey() != null) {
            eventPublisher.publishEvent(new ImageCleanupEvent(List.of(request.imageKey())));
        }
        questionRepository.save(question);
        subjectiveRepository.save(subjective);

        return SubjectiveQuestionCreateResponse.from(subjective);
    }
}
