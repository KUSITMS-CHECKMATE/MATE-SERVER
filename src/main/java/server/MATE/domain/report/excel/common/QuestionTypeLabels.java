package server.MATE.domain.report.excel.common;

import server.MATE.domain.question.entity.QuestionType;

import java.util.Map;

public final class QuestionTypeLabels {

    private static final Map<QuestionType, String> LABELS = Map.of(
            QuestionType.OBJECTIVE, "객관식",
            QuestionType.SUBJECTIVE, "주관식",
            QuestionType.FIVE_SECOND, "5초 테스트",
            QuestionType.SCALE, "척도",
            QuestionType.AB_TEST, "A/B 테스트",
            QuestionType.CARD_SORTING, "카드 소팅",
            QuestionType.TREE_TEST, "트리 테스트"
    );

    private QuestionTypeLabels() {
    }

    public static String label(QuestionType type) {
        return LABELS.getOrDefault(type, type.name());
    }
}
