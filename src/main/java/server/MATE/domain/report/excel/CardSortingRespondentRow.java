package server.MATE.domain.report.excel;

public record CardSortingRespondentRow(
        Long respondentNumber,
        String categoryName,
        int cardNumber
) {
}
