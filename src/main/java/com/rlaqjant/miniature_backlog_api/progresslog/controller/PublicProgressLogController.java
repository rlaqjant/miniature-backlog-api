package com.rlaqjant.miniature_backlog_api.progresslog.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogPageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.service.ProgressLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 공개 진행 로그 컨트롤러 (인증 불필요)
 */
@RestController
@RequestMapping("/public/progress-logs")
@RequiredArgsConstructor
@Validated
public class PublicProgressLogController {

    private final ProgressLogService progressLogService;

    /**
     * 공개 게시판 조회
     * GET /public/progress-logs?page={page}&size={size}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressLogPageResponse>> getPublicProgressLogs(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") int size
    ) {
        ProgressLogPageResponse response = progressLogService.getPublicProgressLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
