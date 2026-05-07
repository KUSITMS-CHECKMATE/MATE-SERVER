package server.MATE.domain.question.service.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.question.dto.request.CardSortingCreateRequest;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardSortingQuestionCreateHandlerTest {

    @Mock
    private CardSortingRepository cardSortingRepository;

    @Test
    @DisplayName("카드소팅 상세 엔티티를 저장하고 카드 목록을 유지한다")
    void savesCardSortingDetailAndPreservesCards() {
        CardSortingQuestionCreateHandler handler = new CardSortingQuestionCreateHandler(cardSortingRepository);
        CardSortingCreateRequest request = new CardSortingCreateRequest(
                "카드소팅",
                "설명",
                List.of("검색", "결제", "홈", "마이페이지")
        );
        Question question = Question.builder()
                .testId(1L)
                .questionType(QuestionType.CARD_SORTING)
                .title("카드소팅")
                .description("설명")
                .sequence(1L)
                .build();

        handler.createDetail(question, request);

        ArgumentCaptor<CardSorting> captor = ArgumentCaptor.forClass(CardSorting.class);
        verify(cardSortingRepository).save(captor.capture());
        CardSorting saved = captor.getValue();

        assertThat(saved.getQuestion()).isEqualTo(question);
        assertThat(saved.getCards()).containsExactly("검색", "결제", "홈", "마이페이지");
        assertThat(handler.extractImageKeys(request)).isEmpty();
    }
}
