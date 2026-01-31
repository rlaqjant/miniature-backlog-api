package com.rlaqjant.miniature_backlog_api.like.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.like.dto.LikeResponse;
import com.rlaqjant.miniature_backlog_api.like.service.MiniatureLikeService;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 미니어처 좋아요 컨트롤러 (인증 필요)
 */
@RestController
@RequestMapping("/miniatures")
@RequiredArgsConstructor
public class MiniatureLikeController {

    private final MiniatureLikeService miniatureLikeService;

    /**
     * 좋아요 토글
     * POST /miniatures/{id}/like
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        LikeResponse response = miniatureLikeService.toggleLike(userDetails.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
