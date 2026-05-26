package server.MATE.domain.report.service.excel.preparer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.cardsorting.CardSortingReportExcelData;
import server.MATE.domain.report.service.excel.support.ReportExcelExportContext;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CardSortingReportExcelServiceTest {

    @InjectMocks
    private CardSortingReportExcelService cardSortingReportExcelService;

    @Test
    void prepareData는_카드소팅_응답과_통계를_엑셀_데이터로_조립한다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.CARD_SORTING)
                .title("카드소팅 질문")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        CardSorting cardSorting = new CardSorting(
                question,
                List.of("결제", "검색", "홈", "설정"),
                List.of("메인 기능", "부가 기능")
        );
        ReflectionTestUtils.setField(cardSorting, "id", 20L);

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.CARD_SORTING)
                .answer(Map.of(
                        "groups", List.of(
                                Map.of("category", "메인 기능", "cardNames", List.of("결제", "검색", "홈")),
                                Map.of("category", "부가 기능", "cardNames", List.of("설정"))
                        )
                ))
                .build();

        Map<String, Object> cardStat = new LinkedHashMap<>();
        cardStat.put("rank", 1);
        cardStat.put("cardName", "결제");
        cardStat.put("count", 1);
        cardStat.put("ratio", 1.0);
        Map<String, Object> reportResult = Map.of(
                "byCategory", List.of(Map.of("category", "메인 기능", "cards", List.of(cardStat)))
        );

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .reportResultByQuestionId(Map.of(20L, reportResult))
                .answersByQuestionId(Map.of(20L, List.of(answer)))
                .cardSortingByQuestionId(Map.of(20L, cardSorting))
                .build();

        CardSortingReportExcelData data = cardSortingReportExcelService.prepareData(context, 20L);

        assertThat(data.respondents()).isNotEmpty();
        assertThat(data.categoryStats()).hasSize(1);
    }

    @Test
    void 카드소팅이_아니면_prepareData를_허용하지_않는다() {
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식")
                .sequence(1L)
                .build();
        ReflectionTestUtils.setField(question, "id", 20L);

        ReportExcelExportContext context = ReportExcelExportContext.builder()
                .questionById(Map.of(20L, question))
                .build();

        assertThatThrownBy(() -> cardSortingReportExcelService.prepareData(context, 20L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_006));
    }
}
