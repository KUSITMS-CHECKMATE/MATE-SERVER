package server.MATE.domain.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.answer.repository.AnswerRepository;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.AbTestReportExcelData;
import server.MATE.domain.report.excel.AbTestReportExcelWriter;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbTestReportExcelServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AbTestReportExcelWriter abTestReportExcelWriter;

    @InjectMocks
    private AbTestReportExcelService abTestReportExcelService;

    @Test
    void 메이커는_AB테스트_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.AB_TEST)
                .title("AB 질문")
                .sequence(1L)
                .build();

        List<Answer> answers = List.of(
                Answer.builder().participationId(1L).questionId(20L).questionType(QuestionType.AB_TEST)
                        .answer(Map.of("selected", "A")).build(),
                Answer.builder().participationId(2L).questionId(20L).questionType(QuestionType.AB_TEST)
                        .answer(Map.of("selected", "A")).build(),
                Answer.builder().participationId(3L).questionId(20L).questionType(QuestionType.AB_TEST)
                        .answer(Map.of("selected", "B")).build()
        );

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(20L, 10L)).willReturn(Optional.of(question));
        given(answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(20L)).willReturn(answers);
        given(abTestReportExcelWriter.write(any(AbTestReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = abTestReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-abtest-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(abTestReportExcelWriter).write(any(AbTestReportExcelData.class));
    }

    @Test
    void AB테스트가_아니면_다운로드를_허용하지_않는다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();
        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.OBJECTIVE)
                .title("객관식")
                .sequence(1L)
                .build();

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(20L, 10L)).willReturn(Optional.of(question));

        assertThatThrownBy(() -> abTestReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_004));
    }
}
