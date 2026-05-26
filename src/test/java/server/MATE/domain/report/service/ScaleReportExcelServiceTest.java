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
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.QuestionRepository;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.report.dto.response.TestReportExcelDownload;
import server.MATE.domain.report.excel.ScaleReportExcelData;
import server.MATE.domain.report.excel.ScaleReportExcelWriter;
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
class ScaleReportExcelServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ScaleRepository scaleRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ScaleReportExcelWriter scaleReportExcelWriter;

    @InjectMocks
    private ScaleReportExcelService scaleReportExcelService;

    @Test
    void 메이커는_척도_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SCALE)
                .title("척도 질문")
                .sequence(1L)
                .build();

        Scale scale = Scale.builder()
                .range(7)
                .build();

        List<Answer> answers = List.of(
                Answer.builder().participationId(100L).questionId(20L).questionType(QuestionType.SCALE)
                        .answer(Map.of("value", 3)).build(),
                Answer.builder().participationId(101L).questionId(20L).questionType(QuestionType.SCALE)
                        .answer(Map.of("value", 7)).build()
        );

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(20L, 10L)).willReturn(Optional.of(question));
        given(scaleRepository.findById(20L)).willReturn(Optional.of(scale));
        given(answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(20L)).willReturn(answers);
        given(scaleReportExcelWriter.write(any(ScaleReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = scaleReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-scale-report-10-q01.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(scaleReportExcelWriter).write(any(ScaleReportExcelData.class));
    }

    @Test
    void 척도가_아니면_다운로드를_허용하지_않는다() {
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

        assertThatThrownBy(() -> scaleReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_005));
    }
}
