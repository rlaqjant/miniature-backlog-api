package com.rlaqjant.miniature_backlog_api.progresslog.service;

import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogCreateRequest;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogPageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.repository.ProgressLogRepository;
import com.rlaqjant.miniature_backlog_api.user.domain.User;
import com.rlaqjant.miniature_backlog_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 진행 로그 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressLogService {

    private final ProgressLogRepository progressLogRepository;
    private final MiniatureRepository miniatureRepository;
    private final UserRepository userRepository;

    /**
     * 진행 로그 작성
     */
    @Transactional
    public ProgressLogResponse createProgressLog(Long userId, ProgressLogCreateRequest request) {
        // 1. 미니어처 조회 및 소유권 검증
        Miniature miniature = miniatureRepository.findById(request.getMiniatureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        validateOwnership(miniature, userId);

        // 2. 진행 로그 생성
        ProgressLog progressLog = ProgressLog.builder()
                .miniatureId(request.getMiniatureId())
                .userId(userId)
                .content(request.getContent())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        ProgressLog savedLog = progressLogRepository.save(progressLog);
        log.info("진행 로그 생성: id={}, miniatureId={}, userId={}", savedLog.getId(), request.getMiniatureId(), userId);

        // 3. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return ProgressLogResponse.of(savedLog, miniature.getTitle(), user.getNickname());
    }

    /**
     * 내 진행 로그 목록 조회 (특정 미니어처)
     */
    public ProgressLogPageResponse getMyProgressLogs(Long userId, Long miniatureId, int page, int size) {
        // 1. 미니어처 조회 및 소유권 검증
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        validateOwnership(miniature, userId);

        // 2. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 진행 로그 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<ProgressLog> progressLogs = progressLogRepository
                .findByMiniatureIdOrderByCreatedAtDesc(miniatureId, pageable);

        // 4. Response 변환
        Page<ProgressLogResponse> responsePage = progressLogs.map(
                log -> ProgressLogResponse.of(log, miniature.getTitle(), user.getNickname())
        );

        return ProgressLogPageResponse.from(responsePage);
    }

    /**
     * 공개 게시판 조회
     */
    public ProgressLogPageResponse getPublicProgressLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProgressLog> progressLogs = progressLogRepository
                .findByIsPublicTrueOrderByCreatedAtDesc(pageable);

        // 미니어처 ID와 사용자 ID 수집
        Set<Long> miniatureIds = progressLogs.getContent().stream()
                .map(ProgressLog::getMiniatureId)
                .collect(Collectors.toSet());

        Set<Long> userIds = progressLogs.getContent().stream()
                .map(ProgressLog::getUserId)
                .collect(Collectors.toSet());

        // 미니어처와 사용자 정보 일괄 조회
        Map<Long, String> miniatureTitles = miniatureRepository.findAllById(miniatureIds).stream()
                .collect(Collectors.toMap(Miniature::getId, Miniature::getTitle));

        Map<Long, String> userNicknames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        // Response 변환
        Page<ProgressLogResponse> responsePage = progressLogs.map(log ->
                ProgressLogResponse.of(
                        log,
                        miniatureTitles.getOrDefault(log.getMiniatureId(), ""),
                        userNicknames.getOrDefault(log.getUserId(), "")
                )
        );

        return ProgressLogPageResponse.from(responsePage);
    }

    /**
     * 소유권 검증
     */
    private void validateOwnership(Miniature miniature, Long userId) {
        if (!miniature.getUserId().equals(userId)) {
            log.warn("미니어처 접근 권한 없음: miniatureId={}, ownerId={}, requesterId={}",
                    miniature.getId(), miniature.getUserId(), userId);
            throw new BusinessException(ErrorCode.MINIATURE_ACCESS_DENIED);
        }
    }
}
