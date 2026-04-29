package server.MATE.domain.question.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.MATE.domain.question.dto.request.SubjectiveQuestionCreateRequest;
import server.MATE.domain.question.dto.response.SubjectiveQuestionCreateResponse;
import server.MATE.domain.question.service.SubjectiveQuestionService;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.common.exception.ErrorCode;
import server.MATE.global.common.response.ApiResponse;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/tests/{testId}/questions")
@RequiredArgsConstructor
public class SubjectiveQuestionController {

    private final SubjectiveQuestionService subjectiveQuestionService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(value = "/subjective", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<SubjectiveQuestionCreateResponse>> createSubjectiveQuestion(
            @PathVariable Long testId,
            @RequestParam String request,
            @RequestPart(required = false) MultipartFile image,
            // TODO: 인증 구현 후 @AuthenticationPrincipal 등으로 대체
            @RequestHeader("X-User-Id") Long makerId
    ) {
        SubjectiveQuestionCreateRequest createRequest = parse(request);
        validate(createRequest);
        SubjectiveQuestionCreateResponse response =
                subjectiveQuestionService.createSubjectiveQuestion(testId, createRequest, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("주관식 질문이 등록되었습니다.", response));
    }

    private SubjectiveQuestionCreateRequest parse(String request) {
        try {
            return objectMapper.readValue(request, SubjectiveQuestionCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.COMMON_003);
        }
    }

    private void validate(SubjectiveQuestionCreateRequest request) {
        Set<ConstraintViolation<SubjectiveQuestionCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BaseException(ErrorCode.COMMON_002, violations.iterator().next().getMessage());
        }
    }
}
