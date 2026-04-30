package server.MATE.domain.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.MATE.domain.test.dto.request.TestCreateRequest;
import server.MATE.domain.test.dto.response.TestCreateResponse;
import server.MATE.domain.test.service.TestService;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.common.response.ApiResponse;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TestCreateResponse>> createTest(
            @RequestParam String request,
            @RequestPart(required = false) List<MultipartFile> images,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        TestCreateRequest testCreateRequest = parse(request);
        validate(testCreateRequest);
        TestCreateResponse response = testService.createTest(testCreateRequest, images, makerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트가 등록되었습니다.", response));
    }

    private TestCreateRequest parse(String request) {
        try {
            return objectMapper.readValue(request, TestCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.COMMON_003);
        }
    }

    private void validate(TestCreateRequest request) {
        Set<ConstraintViolation<TestCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new BaseException(ErrorCode.COMMON_002, message);
        }
    }
}
