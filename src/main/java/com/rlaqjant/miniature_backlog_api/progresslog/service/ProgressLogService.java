package com.rlaqjant.miniature_backlog_api.progresslog.service;

import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.image.dto.ImageResponse;
import com.rlaqjant.miniature_backlog_api.image.service.ImageService;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogCreateRequest;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogPageResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogResponse;
import com.rlaqjant.miniature_backlog_api.progresslog.dto.ProgressLogUpdateRequest;
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

import java.util.Collections;
import java.util.List;
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
    private final ImageService imageService;

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

        // 4. 새로 생성된 로그이므로 이미지는 빈 배열
        return ProgressLogResponse.of(savedLog, miniature.getTitle(), user.getNickname(), Collections.emptyList());
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

        // 4. Response 변환 (이미지 포함)
        Page<ProgressLogResponse> responsePage = progressLogs.map(progressLog -> {
            List<ImageResponse> images = imageService.getImagesByProgressLogId(progressLog.getId());
            return ProgressLogResponse.of(progressLog, miniature.getTitle(), user.getNickname(), images);
        });

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

        // Response 변환 (공개 URL 사용)
        Page<ProgressLogResponse> responsePage = progressLogs.map(progressLog -> {
            List<ImageResponse> images = imageService.getImagesByProgressLogId(progressLog.getId(), true);
            return ProgressLogResponse.of(
                    progressLog,
                    miniatureTitles.getOrDefault(progressLog.getMiniatureId(), ""),
                    userNicknames.getOrDefault(progressLog.getUserId(), ""),
                    images
            );
        });

        return ProgressLogPageResponse.from(responsePage);
    }

    /**
     * 공개 미니어처의 공개 진행 로그 조회
     */
    public ProgressLogPageResponse getPublicProgressLogsByMiniature(Long miniatureId, int page, int size) {
        // 1. 공개 미니어처인지 확인
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        if (!miniature.getIsPublic()) {
            throw new BusinessException(ErrorCode.MINIATURE_NOT_FOUND);
        }

        // 2. 사용자 닉네임 조회
        User user = userRepository.findById(miniature.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 해당 미니어처의 공개 진행 로그 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<ProgressLog> progressLogs = progressLogRepository
                .findByMiniatureIdAndIsPublicTrueOrderByCreatedAtDesc(miniatureId, pageable);

        // 4. Response 변환 (공개 URL 사용)
        Page<ProgressLogResponse> responsePage = progressLogs.map(progressLog -> {
            List<ImageResponse> images = imageService.getImagesByProgressLogId(progressLog.getId(), true);
            return ProgressLogResponse.of(progressLog, miniature.getTitle(), user.getNickname(), images);
        });

        return ProgressLogPageResponse.from(responsePage);
    }

    /**
     * 진행 로그 수정
     */
    @Transactional
    public ProgressLogResponse updateProgressLog(Long userId, Long logId, ProgressLogUpdateRequest request) {
        // 1. 진행 로그 조회
        ProgressLog progressLog = progressLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROGRESS_LOG_NOT_FOUND));

        // 2. 소유권 검증
        if (!progressLog.getUserId().equals(userId)) {
            log.warn("진행 로그 접근 권한 없음: logId={}, ownerId={}, requesterId={}",
                    logId, progressLog.getUserId(), userId);
            throw new BusinessException(ErrorCode.PROGRESS_LOG_ACCESS_DENIED);
        }

        // 3. 수정
        progressLog.update(request.getContent(), request.getIsPublic());

        // 4. 미니어처 제목, 사용자 닉네임 조회
        Miniature miniature = miniatureRepository.findById(progressLog.getMiniatureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 5. 이미지 포함하여 응답
        List<ImageResponse> images = imageService.getImagesByProgressLogId(progressLog.getId());

        log.info("진행 로그 수정: id={}, userId={}", logId, userId);
        return ProgressLogResponse.of(progressLog, miniature.getTitle(), user.getNickname(), images);
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
