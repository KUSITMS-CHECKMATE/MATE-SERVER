package server.MATE.domain.report.service.pdf;

import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
<<<<<<< HEAD
import org.springframework.web.util.UriComponentsBuilder;
=======
>>>>>>> origin/dev

import reactor.core.publisher.Mono;
import server.MATE.domain.report.config.MatePdfProperties;
import server.MATE.domain.report.dto.response.TestReportPdfDownload;
import server.MATE.domain.report.service.excel.support.ReportExcelExportSupport;
import server.MATE.domain.test.entity.Test;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;

@Service
public class TestReportPdfService {

    private final ReportExcelExportSupport reportExcelExportSupport;
    private final ObjectMapper objectMapper;
    private final MatePdfProperties matePdfProperties;
    private final WebClient matePdfWebClient;

    public TestReportPdfService(
            ReportExcelExportSupport reportExcelExportSupport,
            ObjectMapper objectMapper,
            MatePdfProperties matePdfProperties,
            @Qualifier("matePdfWebClient") WebClient matePdfWebClient
    ) {
        this.reportExcelExportSupport = reportExcelExportSupport;
        this.objectMapper = objectMapper;
        this.matePdfProperties = matePdfProperties;
        this.matePdfWebClient = matePdfWebClient;
    }

    public TestReportPdfDownload export(Long testId, Long makerId, String authorization) {
        Test test = reportExcelExportSupport.requireExportReadyTest(testId, makerId);
        if (authorization == null || authorization.isBlank()) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }

<<<<<<< HEAD
        String uri = UriComponentsBuilder.fromPath("/generate")
                .queryParam("testId", testId)
                .queryParam("title", test.getTitle())
                .build()
                .encode()
                .toUriString();

        String responseBody;
        try {
            responseBody = matePdfWebClient.get()
                    .uri(uri)
=======
        String responseBody;
        try {
            responseBody = matePdfWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/generate")
                            .queryParam("testId", testId)
                            .queryParam("title", test.getTitle())
                            .build())
>>>>>>> origin/dev
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
            byte[] pdfBytes = Base64.getDecoder().decode(base64);
            return new TestReportPdfDownload(pdfBytes, buildFilename(testId));
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new BaseException(BaseErrorCode.REPORT_012);
        }
    }

    private String buildFilename(Long testId) {
        return "mate-report-" + testId + ".pdf";
    }
}
