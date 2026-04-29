package server.MATE.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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
import server.MATE.global.image.ImageService;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class SubjectiveQuestionService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final SubjectiveRepository subjectiveRepository;
    private final ImageService imageService;

    public SubjectiveQuestionCreateResponse createSubjectiveQuestion(
            Long testId,
            SubjectiveQuestionCreateRequest request,
            MultipartFile image
    ) {
        validateTestExists(testId);
        validateImage(image);

        String imageKey = null;
        try {
            imageKey = uploadImage(image);
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
                    .imageKey(imageKey)
                    .build();

            questionRepository.save(question);
            subjectiveRepository.save(subjective);

            return SubjectiveQuestionCreateResponse.from(subjective);
        } catch (Exception e) {
            if (imageKey != null) {
                imageService.deleteFiles(List.of(imageKey));
            }
            throw e;
        }
    }

    private void validateTestExists(Long testId) {
        if (!testRepository.existsById(testId)) {
            throw new BaseException(ErrorCode.TEST_004);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new BaseException(ErrorCode.TEST_003);
        }
    }

    private String uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return imageService.uploadFiles(List.of(image)).get(0);
    }
}
