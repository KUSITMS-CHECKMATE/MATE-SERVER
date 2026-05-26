package server.MATE.domain.report.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import server.MATE.domain.question.dto.response.QuestionSummaryItem;
import server.MATE.domain.question.entity.QuestionType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MateReportExcelWriterTest {

    private final MateReportExcelWriter writer = new MateReportExcelWriter();

    @Test
    void 질문_14개면_Q14까지_행이_생성된다() throws Exception {
        TestReportExcelData data = new TestReportExcelData(
                "테스트명",
                "설명",
                "2026.05.01 ~ 진행 중",
                100,
                createQuestions(14)
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("기본 정보");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("테스트 기본정보");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("질문 목록");

            Row firstQuestionRow = sheet.getRow(8);
            assertThat(firstQuestionRow.getCell(0).getStringCellValue()).isEqualTo("Q01");
            assertThat(firstQuestionRow.getCell(1).getStringCellValue()).isEqualTo("객관식");

            Row lastQuestionRow = sheet.getRow(21);
            assertThat(lastQuestionRow.getCell(0).getStringCellValue()).isEqualTo("Q14");
            assertThat(lastQuestionRow.getCell(2).getStringCellValue()).isEqualTo("질문 14");
            assertThat(sheet.getRow(22)).isNull();
        }
    }

    @Test
    void 질문_3개면_질문_행이_3개만_생성된다() throws Exception {
        TestReportExcelData data = new TestReportExcelData(
                "짧은 테스트",
                "설명",
                "2026.05.01 ~ 2026.05.20",
                50,
                createQuestions(3)
        );

        byte[] bytes = writer.write(data);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(8).getCell(0).getStringCellValue()).isEqualTo("Q01");
            assertThat(sheet.getRow(10).getCell(0).getStringCellValue()).isEqualTo("Q03");
            assertThat(sheet.getRow(11)).isNull();
        }
    }

    private List<QuestionSummaryItem> createQuestions(int count) {
        List<QuestionSummaryItem> questions = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            questions.add(new QuestionSummaryItem(
                    (long) index,
                    (long) index,
                    "질문 " + index,
                    QuestionType.OBJECTIVE
            ));
        }
        return questions;
    }
}
