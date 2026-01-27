package com.rlaqjant.miniature_backlog_api.miniature.service;

import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItem;
import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemResponse;
import com.rlaqjant.miniature_backlog_api.backlogitem.repository.BacklogItemRepository;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.image.domain.Image;
import com.rlaqjant.miniature_backlog_api.image.repository.ImageRepository;
import com.rlaqjant.miniature_backlog_api.image.service.ImageService;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureCreateRequest;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureDetailResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureResponse;
import com.rlaqjant.miniature_backlog_api.miniature.dto.MiniatureUpdateRequest;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import com.rlaqjant.miniature_backlog_api.progresslog.repository.ProgressLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 미니어처 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiniatureService {

    private final MiniatureRepository miniatureRepository;
    private final BacklogItemRepository backlogItemRepository;
    private final ProgressLogRepository progressLogRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;

    // 기본 백로그 항목 이름
    private static final List<String> DEFAULT_BACKLOG_STEPS = Arrays.asList(
            "베이스 코트",
            "음영 처리",
            "드라이브러시",
            "마무리"
    );

    /**
     * 내 백로그 목록 조회
     */
    public List<MiniatureResponse> getMyMiniatures(Long userId) {
        List<Miniature> miniatures = miniatureRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return miniatures.stream()
                .map(miniature -> {
                    int progress = calculateProgress(miniature.getId());
                    return MiniatureResponse.of(miniature, progress);
                })
                .toList();
    }

    /**
     * 백로그 생성
     */
    @Transactional
    public MiniatureDetailResponse createMiniature(Long userId, MiniatureCreateRequest request) {
        // 1. Miniature 생성
        Miniature miniature = Miniature.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        Miniature savedMiniature = miniatureRepository.save(miniature);
        log.info("미니어처 생성 완료: id={}, userId={}", savedMiniature.getId(), userId);

        // 2. 기본 BacklogItem 생성
        List<BacklogItem> backlogItems = createDefaultBacklogItems(savedMiniature.getId());

        // 3. Response 생성
        List<BacklogItemResponse> backlogItemResponses = backlogItems.stream()
                .map(BacklogItemResponse::from)
                .toList();

        return MiniatureDetailResponse.of(savedMiniature, 0, backlogItemResponses);
    }

    /**
     * 백로그 상세 조회
     */
    public MiniatureDetailResponse getMiniatureDetail(Long miniatureId, Long userId) {
        // 1. Miniature 조회
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        // 2. 소유권 검증
        validateOwnership(miniature, userId);

        // 3. BacklogItem 조회
        List<BacklogItem> backlogItems = backlogItemRepository
                .findByMiniatureIdOrderByOrderIndexAsc(miniatureId);

        List<BacklogItemResponse> backlogItemResponses = backlogItems.stream()
                .map(BacklogItemResponse::from)
                .toList();

        // 4. 진행률 계산
        int progress = calculateProgress(miniatureId);

        return MiniatureDetailResponse.of(miniature, progress, backlogItemResponses);
    }

    /**
     * 백로그 수정 (부분 업데이트)
     */
    @Transactional
    public MiniatureDetailResponse updateMiniature(Long miniatureId, Long userId, MiniatureUpdateRequest request) {
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));
        validateOwnership(miniature, userId);

        miniature.update(request.getTitle(), request.getDescription(), request.getIsPublic());

        List<BacklogItem> backlogItems = backlogItemRepository
                .findByMiniatureIdOrderByOrderIndexAsc(miniatureId);
        List<BacklogItemResponse> backlogItemResponses = backlogItems.stream()
                .map(BacklogItemResponse::from)
                .toList();
        int progress = calculateProgress(miniatureId);

        return MiniatureDetailResponse.of(miniature, progress, backlogItemResponses);
    }

    /**
     * 백로그 삭제 (연쇄 삭제: Image → ProgressLog → BacklogItem → Miniature)
     */
    @Transactional
    public void deleteMiniature(Long miniatureId, Long userId) {
        // 1. Miniature 조회 + 소유권 검증
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));
        validateOwnership(miniature, userId);

        // 2. ProgressLog 목록 조회 → progressLogIds 수집
        List<ProgressLog> progressLogs = progressLogRepository.findByMiniatureId(miniatureId);
        List<Long> progressLogIds = progressLogs.stream()
                .map(ProgressLog::getId)
                .toList();

        // 3. Image 목록 조회 (progressLogIds) → objectKeys 수집
        List<String> objectKeysToDelete = new ArrayList<>();
        if (!progressLogIds.isEmpty()) {
            List<Image> images = imageRepository.findByProgressLogIdIn(progressLogIds);
            objectKeysToDelete = images.stream()
                    .map(Image::getObjectKey)
                    .toList();

            // 4-1. DB 삭제: Image
            imageRepository.deleteByProgressLogIdIn(progressLogIds);
        }

        // 4-2. DB 삭제: ProgressLog
        progressLogRepository.deleteByMiniatureId(miniatureId);

        // 4-3. DB 삭제: BacklogItem
        backlogItemRepository.deleteByMiniatureId(miniatureId);

        // 4-4. DB 삭제: Miniature
        miniatureRepository.delete(miniature);

        // 5. R2 오브젝트 삭제 (best-effort, 실패해도 DB 삭제 유지)
        if (!objectKeysToDelete.isEmpty()) {
            imageService.deleteR2Objects(objectKeysToDelete);
        }

        log.info("미니어처 삭제 완료: miniatureId={}, userId={}, R2 대상 오브젝트 {}건",
                miniatureId, userId, objectKeysToDelete.size());
    }

    /**
     * 기본 백로그 항목 생성
     */
    private List<BacklogItem> createDefaultBacklogItems(Long miniatureId) {
        List<BacklogItem> items = new ArrayList<>();

        for (int i = 0; i < DEFAULT_BACKLOG_STEPS.size(); i++) {
            BacklogItem item = BacklogItem.builder()
                    .miniatureId(miniatureId)
                    .stepName(DEFAULT_BACKLOG_STEPS.get(i))
                    .status(BacklogItemStatus.TODO)
                    .orderIndex(i)
                    .build();
            items.add(item);
        }

        return backlogItemRepository.saveAll(items);
    }

    /**
     * 진행률 계산 (DONE 개수 / 전체 개수 * 100)
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
