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
import server.MATE.domain.report.excel.SubjectiveReportExcelData;
import server.MATE.domain.report.excel.SubjectiveReportExcelWriter;
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
class SubjectiveReportExcelServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private SubjectiveReportExcelWriter subjectiveReportExcelWriter;

    @InjectMocks
    private SubjectiveReportExcelService subjectiveReportExcelService;

    @Test
    void 메이커는_주관식_통계_엑셀을_다운로드할_수_있다() {
        server.MATE.domain.test.entity.Test test = server.MATE.domain.test.entity.Test.builder()
                .makerId(1L)
                .title("테스트")
                .build();

        Question question = Question.builder()
                .testId(10L)
                .questionType(QuestionType.SUBJECTIVE)
                .title("주관식 질문")
                .sequence(2L)
                .build();

        Answer answer = Answer.builder()
                .participationId(100L)
                .questionId(20L)
                .questionType(QuestionType.SUBJECTIVE)
                .answer(Map.of("text", "좋아요"))
                .build();

        given(testRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(test));
        given(questionRepository.findByIdAndTestIdAndDeletedAtIsNull(20L, 10L)).willReturn(Optional.of(question));
        given(answerRepository.findAllByQuestionIdAndDeletedAtIsNullOrderByParticipationIdAsc(20L))
                .willReturn(List.of(answer));
        given(subjectiveReportExcelWriter.write(any(SubjectiveReportExcelData.class))).willReturn(new byte[]{1, 2, 3});

        TestReportExcelDownload download = subjectiveReportExcelService.export(10L, 20L, 1L);

        assertThat(download.filename()).isEqualTo("mate-subjective-report-10-q02.xlsx");
        assertThat(download.content()).containsExactly(1, 2, 3);
        verify(subjectiveReportExcelWriter).write(any(SubjectiveReportExcelData.class));
    }

    @Test
    void 주관식이_아니면_다운로드를_허용하지_않는다() {
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

        assertThatThrownBy(() -> subjectiveReportExcelService.export(10L, 20L, 1L))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.REPORT_003));
    }
}
