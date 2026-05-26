package server.MATE.domain.report.excel.cardsorting;

public record CardSortingRespondentRow(
        Long respondentNumber,
        String categoryName,
        int cardNumber
) {
}
