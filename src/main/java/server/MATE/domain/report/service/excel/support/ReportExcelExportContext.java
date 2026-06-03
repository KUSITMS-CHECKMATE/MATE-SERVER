package server.MATE.domain.report.service.excel.support;

import server.MATE.domain.answer.entity.Answer;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.CardSorting;
import server.MATE.domain.question.entity.FiveSecond;
import server.MATE.domain.question.entity.Question;
import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.question.entity.Scale;
import server.MATE.domain.question.entity.TreeTest;
import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 한 번의 엑셀 export 요청 동안 메모리에 유지하는 조회 결과 묶음.
 * Redis 등 외부 캐시가 아니라, bulk 조회로 DB 왕복을 줄이기 위한 요청 스코프 데이터다.
 */
public final class ReportExcelExportContext {

    private final Test test;
    private final long testId;
    private final List<QuestionSummaryItem> questionSummaries;
    private final Map<Long, Question> questionById;
    private final Map<Long, Map<String, Object>> reportResultByQuestionId;
    private final Map<Long, List<Answer>> answersByQuestionId;
    private final Map<Long, FiveSecond> fiveSecondByQuestionId;
    private final Map<Long, Scale> scaleByQuestionId;
    private final Map<Long, CardSorting> cardSortingByQuestionId;
    private final Map<Long, Map<Long, TreeTest>> treeNodeByIdByQuestionId;

    ReportExcelExportContext(
            Test test,
            long testId,
            List<QuestionSummaryItem> questionSummaries,
            Map<Long, Question> questionById,
            Map<Long, Map<String, Object>> reportResultByQuestionId,
            Map<Long, List<Answer>> answersByQuestionId,
            Map<Long, FiveSecond> fiveSecondByQuestionId,
            Map<Long, Scale> scaleByQuestionId,
            Map<Long, CardSorting> cardSortingByQuestionId,
            Map<Long, Map<Long, TreeTest>> treeNodeByIdByQuestionId
    ) {
        this.test = test;
        this.testId = testId;
        this.questionSummaries = List.copyOf(questionSummaries);
        this.questionById = Map.copyOf(questionById);
        this.reportResultByQuestionId = Map.copyOf(reportResultByQuestionId);
        this.answersByQuestionId = Map.copyOf(answersByQuestionId);
        this.fiveSecondByQuestionId = Map.copyOf(fiveSecondByQuestionId);
        this.scaleByQuestionId = Map.copyOf(scaleByQuestionId);
        this.cardSortingByQuestionId = Map.copyOf(cardSortingByQuestionId);
        this.treeNodeByIdByQuestionId = Map.copyOf(treeNodeByIdByQuestionId);
    }

    public Test test() {
        return test;
    }

    public long testId() {
        return testId;
    }

    public List<QuestionSummaryItem> questionSummaries() {
        return questionSummaries;
    }

    public Question requireQuestion(Long questionId, QuestionType expectedType, BaseErrorCode wrongTypeErrorCode) {
        Question question = questionById.get(questionId);
        if (question == null) {
            throw new BaseException(BaseErrorCode.QUESTION_005);
        }
        if (question.getQuestionType() != expectedType) {
            throw new BaseException(wrongTypeErrorCode);
        }
        return question;
    }

    public Map<String, Object> requireReportResult(Long questionId, QuestionType questionType) {
        Map<String, Object> reportResult = reportResultByQuestionId.get(questionId);
        if (reportResult == null) {
            throw new BaseException(BaseErrorCode.REPORT_010);
        }
        ReportExcelResultMapper.validateReportResult(questionType, reportResult);
        return reportResult;
    }

    public void assertReportExists(Long questionId) {
        if (!reportResultByQuestionId.containsKey(questionId)) {
            throw new BaseException(BaseErrorCode.REPORT_010);
        }
    }

    public List<Answer> answers(Long questionId) {
        return answersByQuestionId.getOrDefault(questionId, List.of());
    }

    public FiveSecond requireFiveSecond(Long questionId) {
        FiveSecond fiveSecond = fiveSecondByQuestionId.get(questionId);
        if (fiveSecond == null) {
            throw new BaseException(BaseErrorCode.QUESTION_005);
        }
        return fiveSecond;
    }

    public Scale requireScale(Long questionId) {
        Scale scale = scaleByQuestionId.get(questionId);
        if (scale == null) {
            throw new BaseException(BaseErrorCode.QUESTION_005);
        }
        return scale;
    }

    public CardSorting requireCardSorting(Long questionId) {
        CardSorting cardSorting = cardSortingByQuestionId.get(questionId);
        if (cardSorting == null) {
            throw new BaseException(BaseErrorCode.QUESTION_005);
        }
        return cardSorting;
    }

    public Map<Long, TreeTest> treeNodesByQuestionId(Long questionId) {
        return treeNodeByIdByQuestionId.getOrDefault(questionId, Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Test test;
        private long testId;
        private List<QuestionSummaryItem> questionSummaries = List.of();
        private Map<Long, Question> questionById = Map.of();
        private Map<Long, Map<String, Object>> reportResultByQuestionId = Map.of();
        private Map<Long, List<Answer>> answersByQuestionId = Map.of();
        private Map<Long, FiveSecond> fiveSecondByQuestionId = Map.of();
        private Map<Long, Scale> scaleByQuestionId = Map.of();
        private Map<Long, CardSorting> cardSortingByQuestionId = Map.of();
        private Map<Long, Map<Long, TreeTest>> treeNodeByIdByQuestionId = Map.of();

        public Builder test(Test test) {
            this.test = test;
            return this;
        }

        public Builder testId(long testId) {
            this.testId = testId;
            return this;
        }

        public Builder questionSummaries(List<QuestionSummaryItem> questionSummaries) {
            this.questionSummaries = questionSummaries;
            return this;
        }

        public Builder questionById(Map<Long, Question> questionById) {
            this.questionById = questionById;
            return this;
        }

        public Builder reportResultByQuestionId(Map<Long, Map<String, Object>> reportResultByQuestionId) {
            this.reportResultByQuestionId = reportResultByQuestionId;
            return this;
        }

        public Builder answersByQuestionId(Map<Long, List<Answer>> answersByQuestionId) {
            this.answersByQuestionId = answersByQuestionId;
            return this;
        }

        public Builder fiveSecondByQuestionId(Map<Long, FiveSecond> fiveSecondByQuestionId) {
            this.fiveSecondByQuestionId = fiveSecondByQuestionId;
            return this;
        }

        public Builder scaleByQuestionId(Map<Long, Scale> scaleByQuestionId) {
            this.scaleByQuestionId = scaleByQuestionId;
            return this;
        }

        public Builder cardSortingByQuestionId(Map<Long, CardSorting> cardSortingByQuestionId) {
            this.cardSortingByQuestionId = cardSortingByQuestionId;
            return this;
        }

        public Builder treeNodeByIdByQuestionId(Map<Long, Map<Long, TreeTest>> treeNodeByIdByQuestionId) {
            this.treeNodeByIdByQuestionId = treeNodeByIdByQuestionId;
            return this;
        }

        public ReportExcelExportContext build() {
            return new ReportExcelExportContext(
                    test,
                    testId,
                    questionSummaries,
                    questionById,
                    reportResultByQuestionId,
                    answersByQuestionId,
                    fiveSecondByQuestionId,
                    scaleByQuestionId,
                    cardSortingByQuestionId,
                    treeNodeByIdByQuestionId
            );
        }
    }
}
