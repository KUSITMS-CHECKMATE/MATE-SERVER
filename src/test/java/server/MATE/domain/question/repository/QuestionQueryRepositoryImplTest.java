package server.MATE.domain.question.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import server.MATE.domain.question.dto.response.AnswerQuestionTypeView;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.global.config.ClockConfig;
import server.MATE.global.config.JpaAuditingConfig;
import server.MATE.global.config.QuerydslConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, ClockConfig.class, JpaAuditingConfig.class})
class QuestionQueryRepositoryImplTest {

    private static final Long TEST_ID = 10L;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestEntityManager em;

    private Question savedQuestion;

    @BeforeEach
    void setUp() {
        savedQuestion = em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SUBJECTIVE)
                .title("첫 번째 질문")
                .description("설명")
                .sequence(1L)
                .build());
        em.clear();
    }

    @Test
    @DisplayName("findQuestionByIdInTest: 삭제되지 않은 질문을 테스트 범위 안에서 조회한다")
    void findQuestionByIdInTest_returnsQuestion() {
        Optional<Question> result = questionRepository.findQuestionByIdInTest(savedQuestion.getId(), TEST_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedQuestion.getId());
    }

    @Test
    @DisplayName("findQuestionByIdInTest: 필수 인자가 없으면 빈 Optional을 반환한다")
    void findQuestionByIdInTest_missingRequiredArgs_returnsEmpty() {
        assertThat(questionRepository.findQuestionByIdInTest(null, TEST_ID)).isEmpty();
        assertThat(questionRepository.findQuestionByIdInTest(savedQuestion.getId(), null)).isEmpty();
    }

    @Test
    @DisplayName("findQuestionByIdInTest: 삭제된 질문은 조회하지 않는다")
    void findQuestionByIdInTest_deletedQuestion_returnsEmpty() {
        em.getEntityManager()
                .createQuery("update Question q set q.deletedAt = :now where q.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", savedQuestion.getId())
                .executeUpdate();
        em.clear();

        Optional<Question> result = questionRepository.findQuestionByIdInTest(savedQuestion.getId(), TEST_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("countQuestionsInTest: 삭제되지 않은 질문 수만 반환한다")
    void countQuestionsInTest_returnsActiveQuestionCount() {
        em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.OBJECTIVE)
                .title("두 번째 질문")
                .description("설명")
                .sequence(2L)
                .build());
        Question deletedQuestion = em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SCALE)
                .title("삭제된 질문")
                .description("설명")
                .sequence(3L)
                .build());
        em.getEntityManager()
                .createQuery("update Question q set q.deletedAt = :now where q.id = :id")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", deletedQuestion.getId())
                .executeUpdate();
        em.clear();

        long result = questionRepository.countQuestionsInTest(TEST_ID);

        assertThat(result).isEqualTo(2L);
    }

    @Test
    @DisplayName("countQuestionsInTest: testId가 없으면 0을 반환한다")
    void countQuestionsInTest_missingTestId_returnsZero() {
        assertThat(questionRepository.countQuestionsInTest(null)).isZero();
    }

    @Test
    @DisplayName("findQuestionsInTest: sequence 오름차순으로 질문 목록을 반환한다")
    void findQuestionsInTest_returnsQuestionsOrderedBySequence() {
        em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.OBJECTIVE)
                .title("세 번째 질문")
                .description("설명")
                .sequence(3L)
                .build());
        em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.SCALE)
                .title("두 번째 질문")
                .description("설명")
                .sequence(2L)
                .build());
        em.clear();

        List<Question> result = questionRepository.findQuestionsInTest(TEST_ID);

        assertThat(result).extracting(Question::getSequence).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("findQuestionsInTest: testId가 없으면 빈 리스트를 반환한다")
    void findQuestionsInTest_missingTestId_returnsEmpty() {
        assertThat(questionRepository.findQuestionsInTest(null)).isEmpty();
    }

    @Test
    @DisplayName("findAnswerQuestionTypesInTest: 질문 id와 유형 projection을 반환한다")
    void findAnswerQuestionTypesInTest_returnsProjection() {
        List<AnswerQuestionTypeView> result = questionRepository.findAnswerQuestionTypesInTest(TEST_ID);

        assertThat(result).containsExactly(new AnswerQuestionTypeView(savedQuestion.getId(), QuestionType.SUBJECTIVE));
    }

    @Test
    @DisplayName("findQuestionSummariesInTest: 질문 요약 projection을 sequence 순으로 반환한다")
    void findQuestionSummariesInTest_returnsProjectionOrderedBySequence() {
        em.persistAndFlush(Question.builder()
                .testId(TEST_ID)
                .questionType(QuestionType.OBJECTIVE)
                .title("두 번째 질문")
                .description("설명")
                .sequence(2L)
                .build());
        em.clear();

        List<QuestionSummaryItem> result = questionRepository.findQuestionSummariesInTest(TEST_ID);

        assertThat(result).extracting(QuestionSummaryItem::sequence).containsExactly(1L, 2L);
        assertThat(result).extracting(QuestionSummaryItem::title).containsExactly("첫 번째 질문", "두 번째 질문");
    }

    @Test
    @DisplayName("projection 조회 메서드: testId가 없으면 빈 리스트를 반환한다")
    void projectionQueries_missingTestId_returnsEmpty() {
        assertThat(questionRepository.findAnswerQuestionTypesInTest(null)).isEmpty();
        assertThat(questionRepository.findQuestionSummariesInTest(null)).isEmpty();
    }
}
