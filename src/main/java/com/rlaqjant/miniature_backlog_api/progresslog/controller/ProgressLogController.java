package com.rlaqjant.miniature_backlog_api.progresslog.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogCreateRequest;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogPageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.service.ProgressLogService;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 진행 로그 컨트롤러 (인증 필요)
 */
@RestController
@RequestMapping("/progress-logs")
@RequiredArgsConstructor
@Validated
public class ProgressLogController {

    private final ProgressLogService progressLogService;

    /**
     * 진행 로그 작성
     * POST /progress-logs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProgressLogResponse>> createProgressLog(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProgressLogCreateRequest request
    ) {
        ProgressLogResponse response = progressLogService.createProgressLog(
                userDetails.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("진행 로그가 작성되었습니다.", response));
    }

    /**
     * 내 진행 로그 목록 조회 (특정 미니어처)
     * GET /progress-logs?miniatureId={id}&page={page}&size={size}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProgressLogPageResponse>> getMyProgressLogs(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long miniatureId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.") int size
    ) {
        ProgressLogPageResponse response = progressLogService.getMyProgressLogs(
                userDetails.getUserId(), miniatureId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
