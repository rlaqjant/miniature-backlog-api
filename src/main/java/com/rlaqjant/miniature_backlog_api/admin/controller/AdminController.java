package com.rlaqjant.miniature_backlog_api.admin.controller;

import com.rlaqjant.miniature_backlog_api.admin.dto.*;
import com.rlaqjant.miniature_backlog_api.admin.service.AdminService;
import com.rlaqjant.miniature_backlog_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 컨트롤러
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 미니어처 목록 조회 (제목/작성자 분리 검색)
     * GET /admin/miniatures?page=0&size=20&title=제목&author=작성자
     */
    @GetMapping("/miniatures")
    public ResponseEntity<ApiResponse<AdminMiniaturePageResponse>> getMiniatures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {
        AdminMiniaturePageResponse response = adminService.getMiniatures(page, size, title, author);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 미니어처 수정 (공개 여부 토글)
     * PATCH /admin/miniatures/{id}
     */
    @PatchMapping("/miniatures/{id}")
    public ResponseEntity<ApiResponse<Void>> updateMiniature(
            @PathVariable Long id,
            @RequestBody AdminMiniatureUpdateRequest request) {
        adminService.updateMiniature(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 미니어처 삭제
     * DELETE /admin/miniatures/{id}
     */
    @DeleteMapping("/miniatures/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMiniature(@PathVariable Long id) {
        adminService.deleteMiniature(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 사용자 목록 조회 (이메일/닉네임 분리 검색)
     * GET /admin/users?page=0&size=20&email=이메일&nickname=닉네임
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AdminUserPageResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname) {
        AdminUserPageResponse response = adminService.getUsers(page, size, email, nickname);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 수정 (역할 변경)
     * PATCH /admin/users/{id}
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {
        adminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 사용자 삭제 (미니어처 연쇄 삭제)
     * DELETE /admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
