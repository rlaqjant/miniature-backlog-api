package com.rlaqjant.miniature_backlog_api.miniature.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureCreateRequest;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureDetailResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureUpdateRequest;
import com.rlaqjant.miniature_backlog_api.miniature.service.MiniatureService;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 미니어처 컨트롤러
 */
@RestController
@RequestMapping("/miniatures")
@RequiredArgsConstructor
public class MiniatureController {

    private final MiniatureService miniatureService;

    /**
     * 내 백로그 목록 조회
     * GET /miniatures
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MiniatureResponse>>> getMyMiniatures(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MiniatureResponse> miniatures = miniatureService.getMyMiniatures(
                userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(miniatures));
    }

    /**
     * 백로그 생성
     * POST /miniatures
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MiniatureDetailResponse>> createMiniature(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MiniatureCreateRequest request
    ) {
        MiniatureDetailResponse response = miniatureService.createMiniature(
                userDetails.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("백로그가 생성되었습니다.", response));
    }

    /**
     * 백로그 상세 조회
     * GET /miniatures/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MiniatureDetailResponse>> getMiniatureDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        MiniatureDetailResponse response = miniatureService.getMiniatureDetail(
                id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 백로그 수정 (부분 업데이트)
     * PATCH /miniatures/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<MiniatureDetailResponse>> updateMiniature(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody MiniatureUpdateRequest request
    ) {
        MiniatureDetailResponse response = miniatureService.updateMiniature(
                id, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("백로그가 수정되었습니다.", response));
    }

    /**
     * 백로그 삭제
     * DELETE /miniatures/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMiniature(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        miniatureService.deleteMiniature(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
