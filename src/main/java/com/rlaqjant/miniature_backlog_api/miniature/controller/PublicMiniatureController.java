package com.rlaqjant.miniature_backlog_api.miniature.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.PublicMiniatureDetailResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.PublicMiniaturePageResponse;
import com.rlaqjant.miniature_backlog_api.miniature.service.MiniatureService;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogPageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.service.ProgressLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 공개 미니어처 컨트롤러 (인증 불필요)
 */
@RestController
@RequestMapping("/public/miniatures")
@RequiredArgsConstructor
@Validated
public class PublicMiniatureController {

    private final MiniatureService miniatureService;
    private final ProgressLogService progressLogService;

    /**
     * 공개 미니어처 목록 조회
     * GET /public/miniatures?page={page}&size={size}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PublicMiniaturePageResponse>> getPublicMiniatures(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "12") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") int size
    ) {
        PublicMiniaturePageResponse response = miniatureService.getPublicMiniatures(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공개 미니어처 상세 조회
     * GET /public/miniatures/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicMiniatureDetailResponse>> getPublicMiniatureDetail(
            @PathVariable Long id
    ) {
        PublicMiniatureDetailResponse response = miniatureService.getPublicMiniatureDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공개 미니어처의 공개 진행 로그 조회
     * GET /public/miniatures/{id}/progress-logs?page={page}&size={size}
     */
    @GetMapping("/{id}/progress-logs")
    public ResponseEntity<ApiResponse<ProgressLogPageResponse>> getPublicProgressLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") int size
    ) {
        ProgressLogPageResponse response = progressLogService.getPublicProgressLogsByMiniature(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
