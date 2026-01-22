package com.rlaqjant.miniature_backlog_api.backlogitem.controller;

import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemResponse;
import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemUpdateRequest;
import com.rlaqjant.miniature_backlog_api.backlogitem.service.BacklogItemService;
import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 백로그 항목 컨트롤러
 */
@RestController
@RequestMapping("/backlog-items")
@RequiredArgsConstructor
public class BacklogItemController {

    private final BacklogItemService backlogItemService;

    /**
     * 백로그 항목 상태 변경
     * PATCH /backlog-items/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BacklogItemResponse>> updateStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody BacklogItemUpdateRequest request
    ) {
        BacklogItemResponse response = backlogItemService.updateStatus(
                id, request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다.", response));
    }
}
