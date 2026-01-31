package com.rlaqjant.miniature_backlog_api.admin.service;

import com.rlaqjant.miniature_backlog_api.admin.dto.*;
import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import com.rlaqjant.miniature_backlog_api.backlogitem.repository.BacklogItemRepository;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import com.rlaqjant.miniature_backlog_api.miniature.service.MiniatureService;
import com.rlaqjant.miniature_backlog_api.user.domain.User;
import com.rlaqjant.miniature_backlog_api.user.domain.UserRole;
import com.rlaqjant.miniature_backlog_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final MiniatureRepository miniatureRepository;
    private final MiniatureService miniatureService;
    private final BacklogItemRepository backlogItemRepository;
    private final UserRepository userRepository;

    /**
     * 전체 미니어처 목록 조회 (페이지네이션, 제목/작성자 분리 검색)
     */
    public AdminMiniaturePageResponse getMiniatures(int page, int size, String title, String author) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Miniature> miniatures;

        boolean hasTitle = title != null && !title.isBlank();
        boolean hasAuthor = author != null && !author.isBlank();

        if (hasTitle && hasAuthor) {
            // 제목 AND 작성자 동시 검색
            List<Long> matchedUserIds = userRepository.findByNicknameContainingIgnoreCase(author)
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                miniatures = Page.empty(pageable);
            } else {
                miniatures = miniatureRepository.searchByTitleAndUserIds(title, matchedUserIds, pageable);
            }
        } else if (hasTitle) {
            miniatures = miniatureRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(title, pageable);
        } else if (hasAuthor) {
            List<Long> matchedUserIds = userRepository.findByNicknameContainingIgnoreCase(author)
                    .stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                miniatures = Page.empty(pageable);
            } else {
                miniatures = miniatureRepository.findByUserIdInOrderByCreatedAtDesc(matchedUserIds, pageable);
            }
        } else {
            miniatures = miniatureRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // 사용자 닉네임 일괄 조회 (N+1 방지)
        Set<Long> userIds = miniatures.getContent().stream()
                .map(Miniature::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> userNicknames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        // 진행률 계산 포함 응답 변환
        Page<AdminMiniatureResponse> responsePage = miniatures.map(miniature -> {
            String nickname = userNicknames.getOrDefault(miniature.getUserId(), "");
            int progress = calculateProgress(miniature.getId());
            return AdminMiniatureResponse.of(miniature, nickname, progress);
        });

        return AdminMiniaturePageResponse.from(responsePage);
    }

    /**
     * 미니어처 수정 (공개 여부 토글)
     */
    @Transactional
    public void updateMiniature(Long miniatureId, AdminMiniatureUpdateRequest request) {
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        miniature.update(null, null, request.getIsPublic());

        log.info("관리자 미니어처 수정: miniatureId={}, isPublic={}", miniatureId, request.getIsPublic());
    }

    /**
     * 미니어처 삭제 (소유권 검증 없음)
     */
    @Transactional
    public void deleteMiniature(Long miniatureId) {
        miniatureService.deleteMiniatureForAdmin(miniatureId);
    }

    /**
     * 전체 사용자 목록 조회 (페이지네이션, 이메일/닉네임 분리 검색)
     */
    public AdminUserPageResponse getUsers(int page, int size, String email, String nickname) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users;

        boolean hasEmail = email != null && !email.isBlank();
        boolean hasNickname = nickname != null && !nickname.isBlank();

        if (hasEmail && hasNickname) {
            users = userRepository.findByEmailContainingIgnoreCaseAndNicknameContainingIgnoreCase(
                    email, nickname, pageable);
        } else if (hasEmail) {
            users = userRepository.findByEmailContainingIgnoreCase(email, pageable);
        } else if (hasNickname) {
            users = userRepository.findByNicknameContainingIgnoreCase(nickname, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        Page<AdminUserResponse> responsePage = users.map(user -> {
            long miniatureCount = miniatureRepository.countByUserId(user.getId());
            return AdminUserResponse.of(user, miniatureCount);
        });

        return AdminUserPageResponse.from(responsePage);
    }

    /**
     * 사용자 역할 변경
     */
    @Transactional
    public void updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getRole() != null) {
            UserRole newRole = UserRole.valueOf(request.getRole());
            user.updateRole(newRole);
            log.info("관리자 사용자 역할 변경: userId={}, role={}", userId, newRole);
        }
    }

    /**
     * 사용자 삭제 (관리자 계정은 삭제 불가, 미니어처 연쇄 삭제)
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.ADMIN_DELETE_NOT_ALLOWED);
        }

        // 해당 사용자의 모든 미니어처 연쇄 삭제
        List<Miniature> miniatures = miniatureRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Miniature miniature : miniatures) {
            miniatureService.deleteMiniatureForAdmin(miniature.getId());
        }

        userRepository.delete(user);
        log.info("관리자 사용자 삭제: userId={}, miniatureCount={}", userId, miniatures.size());
    }

    /**
     * 진행률 계산
     */
    private int calculateProgress(Long miniatureId) {
        long total = backlogItemRepository.countByMiniatureId(miniatureId);
        if (total == 0) {
            return 0;
        }
        long done = backlogItemRepository.countByMiniatureIdAndStatus(
                miniatureId, BacklogItemStatus.DONE);
        return (int) Math.round((double) done / total * 100);
    }
}
