package server.MATE.domain.report.service.pdf;

import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import server.MATE.domain.report.config.MatePdfProperties;
import server.MATE.domain.report.dto.response.TestReportPdfDownload;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.storage.FileStorageService;

@Service
public class TestReportPdfService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final ObjectMapper objectMapper;
    private final MatePdfProperties matePdfProperties;
    private final WebClient matePdfWebClient;
    private final FileStorageService fileStorageService;

    public TestReportPdfService(
            ReportExcelExportSupport reportExcelExportSupport,
            ObjectMapper objectMapper,
            MatePdfProperties matePdfProperties,
            @Qualifier("matePdfWebClient") WebClient matePdfWebClient,
            FileStorageService fileStorageService
    ) {
        this.reportExcelExportSupport = reportExcelExportSupport;
        this.objectMapper = objectMapper;
        this.matePdfProperties = matePdfProperties;
        this.matePdfWebClient = matePdfWebClient;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TestReportPdfDownload export(Long testId, Long makerId, String authorization) {
        Test test = reportExcelExportSupport.requireExportReadyTest(testId, makerId);

        if (test.getPdfKey() != null) {
            return new TestReportPdfDownload(
                    fileStorageService.generateDownloadUrl(test.getPdfKey()),
                    buildFilename(testId)
            );
        }

        if (authorization == null || authorization.isBlank()) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }

        byte[] pdfBytes = generatePdf(testId, test.getTitle(), authorization);
        String pdfKey = "reports/pdf/" + testId + ".pdf";
        fileStorageService.upload(pdfKey, pdfBytes, "application/pdf");
        test.savePdfKey(pdfKey);

        return new TestReportPdfDownload(
                fileStorageService.generateDownloadUrl(pdfKey),
                buildFilename(testId)
        );
    }

    private byte[] generatePdf(Long testId, String title, String authorization) {
        String responseBody;
        try {
            responseBody = matePdfWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/generate")
                            .queryParam("testId", testId)
                            .queryParam("title", title)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new BaseException(BaseErrorCode.REPORT_012))))
                    .bodyToMono(String.class)
                    .block(matePdfProperties.timeout());
        } catch (BaseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String base64 = root.path("data").asText(null);
            if (base64 == null || base64.isBlank()) {
                throw new BaseException(BaseErrorCode.REPORT_012);
            }
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".pdf";
    }
}
