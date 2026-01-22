package com.rlaqjant.miniature_backlog_api.image.controller;

import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import com.rlaqjant.miniature_backlog_api.image.dto.*;
import com.rlaqjant.miniature_backlog_api.image.service.ImageService;
import com.rlaqjant.miniature_backlog_api.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 이미지 컨트롤러
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * Presigned URL 발급
     * 클라이언트가 직접 R2에 업로드할 수 있는 URL 생성
     */
    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<PresignResponse>> generatePresignedUrl(
            @Valid @RequestBody PresignRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PresignResponse response = imageService.generatePresignedUrl(request, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 이미지 메타데이터 저장
     * 클라이언트가 R2 업로드 완료 후 호출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ImageResponse>> saveImage(
            @Valid @RequestBody ImageCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ImageResponse response = imageService.saveImage(request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이미지가 저장되었습니다.", response));
    }
}
