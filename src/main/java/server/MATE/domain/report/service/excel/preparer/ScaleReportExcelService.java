package server.MATE.domain.report.service.excel.preparer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.repository.ScaleRepository;
import server.MATE.domain.report.excel.scale.ScaleReportExcelData;
import server.MATE.domain.report.excel.scale.ScaleRespondentRow;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.report.service.excel.support.ReportExcelResultMapper;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScaleReportExcelService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final ScaleRepository scaleRepository;

    public ScaleReportExcelData prepareData(Long testId, Long questionId, Long makerId) {
        reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        Question question = reportExcelExportSupport.requireQuestion(
                testId, questionId, QuestionType.SCALE, BaseErrorCode.REPORT_005
        );
        Map<String, Object> reportResult = reportExcelExportSupport.requireReportResult(testId, questionId);

        Scale scale = scaleRepository.findById(questionId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.QUESTION_005));

        List<Answer> answers = reportExcelExportSupport.loadAnswers(questionId);
        List<ScaleRespondentRow> respondents = buildRespondentRows(answers);
        ReportExcelResultMapper.ScaleStats scaleStats = ReportExcelResultMapper.toScaleStats(scale.getRange(), reportResult);

        return new ScaleReportExcelData(
                String.format("Q%02d", question.getSequence()),
                question.getTitle(),
                respondents,
                scaleStats.valueStats(),
                scaleStats.average()
        );
    }

    private List<ScaleRespondentRow> buildRespondentRows(List<Answer> answers) {
        List<ScaleRespondentRow> rows = new ArrayList<>();
        for (Answer answer : answers) {
            Integer value = extractScaleValue(answer);
            if (value == null) {
                continue;
            }
            rows.add(new ScaleRespondentRow(answer.getParticipationId(), value));
        }
        return rows;
    }

    private Integer extractScaleValue(Answer answer) {
        Object value = answer.getAnswer().get("value");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
