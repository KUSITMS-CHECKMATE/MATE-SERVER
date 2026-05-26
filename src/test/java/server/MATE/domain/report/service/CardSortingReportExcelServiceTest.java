package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.CardSortingRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.CardSortingReportExcelData;
import server.MATE.domain.report.excel.CardSortingReportExcelWriter;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardSortingReportExcelServiceTest {

    @Mock
    private ReportExcelExportSupport reportExcelExportSupport;

    @Mock
    private CardSortingRepository cardSortingRepository;

    @Mock
    private CardSortingReportExcelWriter cardSortingReportExcelWriter;

    @InjectMocks
    private CardSortingReportExcelService cardSortingReportExcelService;

    @Test
    void 메이커는_카드소팅_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.CARD_SORTING)
                .title("카드소팅 질문")
                .sequence(1L)
                .build();

        CardSorting cardSorting = new CardSorting(
                question,
                List.of("결제", "검색", "홈", "설정"),
                List.of("메인 기능", "부가 기능")
        );

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

        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L)).willReturn(test);
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.CARD_SORTING, BaseErrorCode.REPORT_006))
                .willReturn(question);
        given(reportExcelExportSupport.requireReportResult(10L, 20L)).willReturn(reportResult);
        given(cardSortingRepository.findById(20L)).willReturn(java.util.Optional.of(cardSorting));
        given(reportExcelExportSupport.loadAnswers(20L)).willReturn(List.of(answer));
        given(cardSortingReportExcelWriter.write(any(CardSortingReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = cardSortingReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-card-sorting-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(cardSortingReportExcelWriter).write(any(CardSortingReportExcelData.class));
    }

    @Test
    void 카드소팅이_아니면_다운로드를_허용하지_않는다() {
        given(reportExcelExportSupport.requireExportReadyTest(10L, 1L))
                .willReturn(server.MATE.domain.test.entity.Test.builder().makerId(1L).build());
        given(reportExcelExportSupport.requireQuestion(10L, 20L, QuestionType.CARD_SORTING, BaseErrorCode.REPORT_006))
                .willThrow(new BaseException(BaseErrorCode.REPORT_006));

        assertThatThrownBy(() -> cardSortingReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_006));
    }
}
