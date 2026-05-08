package server.MATE.domain.question.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import server.MATE.domain.question.dto.request.ObjectiveCreateRequest;
import server.MATE.domain.question.dto.request.ObjectiveOptionRequest;
import server.MATE.domain.question.dto.request.QuestionCreateRequest;
import server.MATE.domain.question.dto.request.ScaleCreateRequest;
import server.MATE.domain.question.dto.response.QuestionCreateResponse;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.service.handler.QuestionCreateHandler;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.image.event.ImageCleanupEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private QuestionCreateHandler objectiveHandler;

    @Mock
    private QuestionCreateHandler scaleHandler;

    private QuestionService questionService;

    private static final Long TEST_ID = 10L;
    private static final Long MAKER_ID = 1L;

    private server.MATE.domain.test.entity.Test test;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("테스트")
                .description("설명")
                .serviceName("서비스")
                .serviceDescription("서비스 설명")
                .imageKeys(List.of())
                .build();

        lenient().when(objectiveHandler.supports()).thenReturn(QuestionType.OBJECTIVE);
        lenient().when(scaleHandler.supports()).thenReturn(QuestionType.SCALE);

        questionService = new QuestionService(testRepository, questionRepository, eventPublisher,
                List.of(objectiveHandler, scaleHandler));
        lenient().when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("여러 문항을 요청 순서대로 등록하고 이미지 정리 이벤트를 한 번 발행한다")
    void createQuestionsInRequestOrderAndPublishSingleCleanupEvent() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(5L);

        ObjectiveCreateRequest objective = new ObjectiveCreateRequest(
                "객관식 질문",
                "설명",
                false,
                null,
                null,
                true,
                List.of(
                        new ObjectiveOptionRequest("A", "image-a"),
                        new ObjectiveOptionRequest("B", null)
                )
        );
        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                "image-scale",
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(objective, scale));

        given(objectiveHandler.extractImageKeys(objective)).willReturn(List.of("image-a"));
        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of("image-scale"));

        QuestionCreateResponse response = questionService.createQuestions(TEST_ID, MAKER_ID, request);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(2)).save(questionCaptor.capture());
        List<Question> savedQuestions = questionCaptor.getAllValues();

        assertThat(savedQuestions).hasSize(2);
        assertThat(savedQuestions.get(0).getQuestionType()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(savedQuestions.get(0).getSequence()).isEqualTo(6L);
        assertThat(savedQuestions.get(1).getQuestionType()).isEqualTo(QuestionType.SCALE);
        assertThat(savedQuestions.get(1).getSequence()).isEqualTo(7L);

        verify(objectiveHandler).validate(objective);
        verify(scaleHandler).validate(scale);
        verify(objectiveHandler).createDetail(savedQuestions.get(0), objective);
        verify(scaleHandler).createDetail(savedQuestions.get(1), scale);

        ArgumentCaptor<ImageCleanupEvent> eventCaptor = ArgumentCaptor.forClass(ImageCleanupEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().imageKeys()).containsExactly("image-a", "image-scale");

        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).type()).isEqualTo(QuestionType.OBJECTIVE);
        assertThat(response.questions().get(0).sequence()).isEqualTo(6L);
        assertThat(response.questions().get(1).type()).isEqualTo(QuestionType.SCALE);
        assertThat(response.questions().get(1).sequence()).isEqualTo(7L);
    }

    @Test
    @DisplayName("이미지가 없는 경우 정리 이벤트를 발행하지 않는다")
    void doesNotPublishCleanupEventWhenNoImagesExist() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                null,
                "낮음",
                "높음",
                5
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(scale));

        given(scaleHandler.extractImageKeys(scale)).willReturn(List.of());

        questionService.createQuestions(TEST_ID, MAKER_ID, request);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("테스트가 없으면 TEST_004 예외가 발생한다")
    void throwsTest004WhenTestDoesNotExist() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.empty());

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    @DisplayName("삭제된 테스트면 TEST_004 예외가 발생한다")
    void throwsTest004WhenTestIsDeleted() {
        test.delete(LocalDateTime.now());
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.empty());

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_004);
    }

    @Test
    @DisplayName("제작자가 아니면 TEST_005 예외가 발생한다")
    void throwsTest005WhenMakerDoesNotMatch() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));

        QuestionCreateRequest request = new QuestionCreateRequest(List.of(
                new ScaleCreateRequest("척도 질문", "설명", null, "낮음", "높음", 5)
        ));

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID + 1, request))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(BaseErrorCode.TEST_005);
    }

    @Test
    @DisplayName("핸들러 검증에서 실패하면 저장과 이벤트 발행이 중단된다")
    void stopsPersistenceAndEventPublishingWhenHandlerValidationFails() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(questionRepository.findMaxSequenceByTestId(TEST_ID)).willReturn(0L);

        ScaleCreateRequest scale = new ScaleCreateRequest(
                "척도 질문",
                "설명",
                null,
                "낮음",
                "높음",
                9
        );
        QuestionCreateRequest request = new QuestionCreateRequest(List.of(scale));

        BaseException exception = new BaseException(BaseErrorCode.COMMON_002);
        org.mockito.Mockito.doThrow(exception).when(scaleHandler).validate(scale);

        assertThatThrownBy(() -> questionService.createQuestions(TEST_ID, MAKER_ID, request))
                .isSameAs(exception);

        verify(scaleHandler).validate(scale);
        verify(questionRepository, never()).save(any(Question.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
