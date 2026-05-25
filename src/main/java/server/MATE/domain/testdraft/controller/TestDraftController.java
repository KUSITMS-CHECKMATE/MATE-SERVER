package server.MATE.domain.testdraft.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.MATE.domain.testdraft.dto.request.TestDraftUpdateRequest;
import server.MATE.domain.testdraft.dto.response.MyTestDraftResponse;
import server.MATE.domain.testdraft.dto.response.TestDraftResponse;
import server.MATE.domain.testdraft.service.TestDraftService;
import server.MATE.global.common.response.ApiResponse;
import server.MATE.global.security.principal.AuthenticatedUser;

@Tag(name = "[TEST DRAFT] 테스트 초안 API", description = "테스트 초안 관련 API")
@RestController
@RequestMapping("/api/v1/test-drafts")
@SecurityRequirement(name = "JWT")
@RequiredArgsConstructor
public class TestDraftController {

    private final TestDraftService testDraftService;

    @Operation(
            summary = "테스트 초안 등록",
            description = """
                    빈 테스트 초안을 등록하고 draftId를 반환합니다. MKTT_01 화면에 해당하는 api 입니다. +버튼을 눌렀을 때 요청해주세요.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<TestDraftResponse>> createDraft(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestDraftResponse response = testDraftService.createDraft(authenticatedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("테스트 초안을 등록했습니다.", response));
    }

    @Operation(
            summary = "테스트 초안 상세 조회",
            description = """
                    현재 로그인한 사용자가 생성한 테스트 초안을 상세 조회합니다.
                    """
    )
    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<TestDraftResponse>> getDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestDraftResponse response = testDraftService.getDraft(draftId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 초안을 조회했습니다.", response));
    }

    @Operation(summary = "내 테스트 초안 목록 조회", description = "현재 로그인한 사용자가 생성한 테스트 초안 목록을 최신 수정순으로 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyTestDraftResponse>> listMyDrafts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        MyTestDraftResponse response = testDraftService.listMyDrafts(authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("내 테스트 초안 목록을 조회했습니다.", response));
    }

    @Operation(
            summary = "테스트 초안 수정",
            description = """
                    테스트 초안의 기본 정보, 질문 payload, 목표 인원, 리워드를 수정합니다. MKTT_02, MKTT_03, 결제하기 화면에 해당하는 api 입니다.
                    """
    )
    @PatchMapping("/{draftId}")
    public ResponseEntity<ApiResponse<TestDraftResponse>> updateDraft(
            @PathVariable Long draftId,
            @RequestBody @Valid TestDraftUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        TestDraftResponse response = testDraftService.updateDraft(draftId, authenticatedUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("테스트 초안을 수정했습니다.", response));
    }

    @Operation(summary = "테스트 초안 삭제", description = "특정 테스트 초안을 hard delete 합니다.")
    @DeleteMapping("/{draftId}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        testDraftService.deleteDraft(draftId, authenticatedUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("테스트 초안을 삭제했습니다.", null));
    }
}
