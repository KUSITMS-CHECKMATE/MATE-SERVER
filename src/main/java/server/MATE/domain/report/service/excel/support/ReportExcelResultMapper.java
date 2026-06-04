package server.MATE.domain.report.service.excel.support;

import server.MATE.domain.question.entity.QuestionType;
import server.MATE.domain.report.excel.cardsorting.CardSortingCategoryStatRow;
import server.MATE.domain.report.excel.fivesecond.FiveSecondOptionStatRow;
import server.MATE.domain.report.excel.objective.ObjectiveOptionStatRow;
import server.MATE.domain.report.excel.scale.ScaleValueStatRow;
import server.MATE.domain.report.excel.treetest.TreeTestPathStatRow;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReportExcelResultMapper {

    private ReportExcelResultMapper() {
    }

    public static void validateReportResult(QuestionType questionType, Map<String, Object> reportResult) {
        switch (questionType) {
            case OBJECTIVE -> requireList(reportResult, "options");
            case FIVE_SECOND, SUBJECTIVE -> {
                // report row 존재만 확인하고, 형식 검증은 preparer에서 처리한다.
            }
            case AB_TEST -> {
                requireMap(reportResult, "A");
                requireMap(reportResult, "B");
            }
            case SCALE -> {
                requireNumber(reportResult, "average");
                requireList(reportResult, "distribution");
            }
            case CARD_SORTING -> requireList(reportResult, "byCategory");
            case TREE_TEST -> requireList(reportResult, "pathFrequency");
            default -> throw new BaseException(BaseErrorCode.COMMON_002);
        }
    }

    public static Map<Long, String> buildOptionContentFromReport(Map<String, Object> reportResult) {
        Map<Long, String> contentById = new LinkedHashMap<>();
        for (Map<String, Object> optionStat : readOptionStats(reportResult)) {
            Object optionIdObject = optionStat.get("optionId");

            Object contentObject = optionStat.get("content");
            if (optionIdObject instanceof Number n) {
                contentById.put(n.longValue(), contentObject instanceof String s ? s : "");
            }
        }
        return contentById;
    }

    public static List<ObjectiveOptionStatRow> toObjectiveOptionStats(Map<String, Object> reportResult) {
        List<Map<String, Object>> optionStats = readOptionStats(reportResult);
        List<ObjectiveOptionStatRow> stats = new ArrayList<>();
        for (int index = 0; index < optionStats.size(); index++) {
            Map<String, Object> optionStat = optionStats.get(index);
            stats.add(new ObjectiveOptionStatRow(
                    "선지 " + (index + 1),
                    readInt(optionStat.get("count")),
                    formatObjectiveRatioPercent(readDouble(optionStat.get("ratio")))
            ));
        }
        return stats;
    }

    public static List<FiveSecondOptionStatRow> toFiveSecondOptionStats(Map<String, Object> reportResult) {
        List<FiveSecondOptionStatRow> stats = new ArrayList<>();
        for (Map<String, Object> optionStat : readOptionStats(reportResult)) {
            String content = optionStat.get("content") instanceof String s ? s : "";
            stats.add(new FiveSecondOptionStatRow(
                    content,
                    readInt(optionStat.get("count")),
                    formatObjectiveRatioPercent(readDouble(optionStat.get("ratio")))
            ));
        }
        return stats;
    }

    public static AbTestCounts toAbTestCounts(Map<String, Object> reportResult) {
        Map<String, Object> versionA = readVersionMap(reportResult.get("A"));
        Map<String, Object> versionB = readVersionMap(reportResult.get("B"));
        int versionACount = readInt(versionA.get("count"));
        int versionBCount = readInt(versionB.get("count"));
        return new AbTestCounts(versionACount, versionBCount);
    }

    public static ScaleStats toScaleStats(int range, Map<String, Object> reportResult) {
        double average = readDouble(reportResult.get("average"));
        Map<Integer, Integer> countByScore = new HashMap<>();
        for (int score = 1; score <= range; score++) {
            countByScore.put(score, 0);
        }

        for (Map<String, Object> item : readDistribution(reportResult)) {
            int score = readInt(item.get("score"));
            if (score >= 1 && score <= range) {
                countByScore.put(score, readInt(item.get("count")));
            }
        }

        int total = countByScore.values().stream().mapToInt(Integer::intValue).sum();
        List<ScaleValueStatRow> valueStats = new ArrayList<>();
        for (int score = 1; score <= range; score++) {
            int count = countByScore.getOrDefault(score, 0);
            valueStats.add(new ScaleValueStatRow(
                    score,
                    count,
                    formatScaleRatioPercent(count, total)
            ));
        }
        return new ScaleStats(valueStats, formatAverage(average));
    }

    public static List<CardSortingCategoryStatRow> toCardSortingCategoryStats(Map<String, Object> reportResult) {
        Object byCategoryObject = reportResult.get("byCategory");
        if (!(byCategoryObject instanceof List<?> byCategory)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }

        List<CardSortingCategoryStatRow> rows = new ArrayList<>();
        for (Object categoryObject : byCategory) {
            if (!(categoryObject instanceof Map<?, ?> categoryMap)) {
                continue;
            }
            String categoryName = String.valueOf(categoryMap.get("category"));
            Object cardsObject = categoryMap.get("cards");
            if (!(cardsObject instanceof List<?> cards)) {
                continue;
            }
            for (Object cardObject : cards) {
                if (!(cardObject instanceof Map<?, ?> cardMap)) {
                    continue;
                }
                int rank = readInt(cardMap.get("rank"));
                String cardName = String.valueOf(cardMap.get("cardName"));
                double ratio = readDouble(cardMap.get("ratio"));
                rows.add(new CardSortingCategoryStatRow(
                        categoryName,
                        String.format("카드 %d순위 %s", rank, cardName),
                        formatCardSortingRatioPercent(ratio)
                ));
            }
        }
        return rows;
    }

    public static List<TreeTestPathStatRow> toTreeTestPathStats(Map<String, Object> reportResult) {
        Object pathFrequencyObject = reportResult.get("pathFrequency");
        if (!(pathFrequencyObject instanceof List<?> pathFrequency)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object itemObject : pathFrequency) {
            if (itemObject instanceof Map<?, ?> itemMap) {
                items.add(toStringObjectMap(itemMap));
            }
        }

        int total = items.stream().mapToInt(item -> readInt(item.get("count"))).sum();
        List<TreeTestPathStatRow> rows = new ArrayList<>();
        for (Map<String, Object> item : items) {
            int count = readInt(item.get("count"));
            rows.add(new TreeTestPathStatRow(
                    formatTreeTestPathLabel(item.get("pathLabels")),
                    count,
                    formatTreeTestRatioPercent(count, total)
            ));
        }
        return rows;
    }

    public static int readTreeTestTotalResponseCount(Map<String, Object> reportResult) {
        Object pathFrequencyObject = reportResult.get("pathFrequency");
        if (!(pathFrequencyObject instanceof List<?> pathFrequency)) {
            return 0;
        }
        int total = 0;
        for (Object itemObject : pathFrequency) {
            if (itemObject instanceof Map<?, ?> itemMap) {
                total += readInt(itemMap.get("count"));
            }
        }
        return total;
    }

    private static String formatTreeTestPathLabel(Object pathLabelsObject) {
        if (!(pathLabelsObject instanceof List<?> pathLabels)) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (Object labelObject : pathLabels) {
            if (labelObject != null) {
                labels.add(String.valueOf(labelObject));
            }
        }
        return String.join(" > ", labels);
    }

    private static String formatTreeTestRatioPercent(int count, int total) {
        if (total == 0) {
            return "-";
        }
        double percent = count * 100.0 / total;
        if (percent == Math.rint(percent)) {
            return String.valueOf((long) percent);
        }
        return String.format("%.2f", percent);
    }

    private static List<Map<String, Object>> readOptionStats(Map<String, Object> reportResult) {
        Object optionsObject = reportResult.get("options");
        if (!(optionsObject instanceof List<?> options)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object optionObject : options) {
            if (optionObject instanceof Map<?, ?> optionMap) {
                result.add(toStringObjectMap(optionMap));
            }
        }
        return result;
    }

    private static List<Map<String, Object>> readDistribution(Map<String, Object> reportResult) {
        Object distributionObject = reportResult.get("distribution");
        if (!(distributionObject instanceof List<?> distribution)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object itemObject : distribution) {
            if (itemObject instanceof Map<?, ?> itemMap) {
                result.add(toStringObjectMap(itemMap));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readVersionMap(Object versionObject) {
        if (versionObject instanceof Map<?, ?> versionMap) {
            return (Map<String, Object>) versionMap;
        }
        throw new BaseException(BaseErrorCode.REPORT_011);
    }

    private static List<?> requireList(Map<String, Object> reportResult, String key) {
        Object value = reportResult.get(key);
        if (!(value instanceof List<?> list)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }
        return list;
    }

    private static Map<?, ?> requireMap(Map<String, Object> reportResult, String key) {
        Object value = reportResult.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }
        return map;
    }

    private static void requireNumber(Map<String, Object> reportResult, String key) {
        Object value = reportResult.get(key);
        if (!(value instanceof Number)) {
            throw new BaseException(BaseErrorCode.REPORT_011);
        }
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static int readInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static String formatObjectiveRatioPercent(double ratio) {
        double percent = ratio * 100;
        if (percent == Math.rint(percent)) {
            return String.valueOf((long) percent);
        }
        return String.format("%.2f", percent);
    }

    private static String formatScaleRatioPercent(int count, int total) {
        if (total == 0) {
            return "0.00%";
        }
        return String.format("%.2f%%", count * 100.0 / total);
    }

    private static String formatCardSortingRatioPercent(double ratio) {
        return String.format("%.2f%%", ratio * 100);
    }

    private static String formatAverage(double average) {
        if (average == Math.rint(average)) {
            return String.valueOf((long) average);
        }
        return String.format("%.2f", average);
    }

    public record AbTestCounts(int versionACount, int versionBCount) {
    }

    public record ScaleStats(List<ScaleValueStatRow> valueStats, String average) {
    }
}
