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
import com.rlaqjant.miniature_backlog_api.like.repository.MiniatureLikeRepository;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.dto.*;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
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

import java.util.*;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;
    private final MiniatureLikeRepository miniatureLikeRepository;

    // 기본 백로그 항목 이름
    private static final List<String> DEFAULT_BACKLOG_STEPS = Arrays.asList(
            "언박싱",
            "조립",
            "프라이밍",
            "도색",
            "마무리"
    );

    /**
     * 내 백로그 목록 조회 (N+1 → 일괄 조회 최적화)
     */
    public List<MiniatureResponse> getMyMiniatures(Long userId) {
        List<Miniature> miniatures = miniatureRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 미니어처 ID 목록으로 backlogItem 일괄 조회
        List<Long> miniatureIds = miniatures.stream()
                .map(Miniature::getId)
                .toList();

        Map<Long, List<BacklogItem>> backlogItemsMap = backlogItemRepository
                .findByMiniatureIdInOrderByMiniatureIdAscOrderIndexAsc(miniatureIds)
                .stream()
                .collect(Collectors.groupingBy(BacklogItem::getMiniatureId));

        // 미니어처별 최신 썸네일 일괄 조회
        Map<Long, String> thumbnailMap = buildThumbnailMap(miniatureIds, false);

        return miniatures.stream()
                .map(miniature -> {
                    List<BacklogItem> items = backlogItemsMap.getOrDefault(
                            miniature.getId(), Collections.emptyList());
                    int progress = calculateProgressFromItems(items);
                    String currentStep = calculateCurrentStep(items);
                    String thumbnailUrl = thumbnailMap.get(miniature.getId());
                    return MiniatureResponse.of(miniature, progress, currentStep, thumbnailUrl);
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

        // 4-4. DB 삭제: MiniatureLike
        miniatureLikeRepository.deleteByMiniatureId(miniatureId);

        // 4-5. DB 삭제: Miniature
        miniatureRepository.delete(miniature);

        // 5. R2 오브젝트 삭제 (best-effort, 실패해도 DB 삭제 유지)
        if (!objectKeysToDelete.isEmpty()) {
            imageService.deleteR2Objects(objectKeysToDelete);
        }

        log.info("미니어처 삭제 완료: miniatureId={}, userId={}, R2 대상 오브젝트 {}건",
                miniatureId, userId, objectKeysToDelete.size());
    }

    /**
     * 관리자용 미니어처 삭제 (소유권 검증 없음)
     */
    @Transactional
    public void deleteMiniatureForAdmin(Long miniatureId) {
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        // ProgressLog 목록 조회 → progressLogIds 수집
        List<ProgressLog> progressLogs = progressLogRepository.findByMiniatureId(miniatureId);
        List<Long> progressLogIds = progressLogs.stream()
                .map(ProgressLog::getId)
                .toList();

        // Image 목록 조회 → objectKeys 수집
        List<String> objectKeysToDelete = new ArrayList<>();
        if (!progressLogIds.isEmpty()) {
            List<Image> images = imageRepository.findByProgressLogIdIn(progressLogIds);
            objectKeysToDelete = images.stream()
                    .map(Image::getObjectKey)
                    .toList();
            imageRepository.deleteByProgressLogIdIn(progressLogIds);
        }

        progressLogRepository.deleteByMiniatureId(miniatureId);
        backlogItemRepository.deleteByMiniatureId(miniatureId);
        miniatureLikeRepository.deleteByMiniatureId(miniatureId);
        miniatureRepository.delete(miniature);

        // R2 오브젝트 삭제 (best-effort)
        if (!objectKeysToDelete.isEmpty()) {
            imageService.deleteR2Objects(objectKeysToDelete);
        }

        log.info("관리자 미니어처 삭제 완료: miniatureId={}, R2 대상 오브젝트 {}건",
                miniatureId, objectKeysToDelete.size());
    }

    /**
     * 공개 미니어처 목록 조회 (페이지네이션, 좋아요 정보 포함)
     * @param userId 현재 로그인 사용자 ID (null이면 비로그인)
     */
    public PublicMiniaturePageResponse getPublicMiniatures(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Miniature> miniatures = miniatureRepository.findByIsPublicTrueOrderByUpdatedAtDesc(pageable);

        // 사용자 ID 수집 및 일괄 조회
        Set<Long> userIds = miniatures.getContent().stream()
                .map(Miniature::getUserId)
                .collect(Collectors.toSet());

        Map<Long, String> userNicknames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        // 미니어처 ID 목록
        List<Long> miniatureIds = miniatures.getContent().stream()
                .map(Miniature::getId)
                .toList();

        // 좋아요 수 일괄 조회 (N+1 방지)
        Map<Long, Long> likeCountMap = new HashMap<>();
        if (!miniatureIds.isEmpty()) {
            miniatureLikeRepository.countByMiniatureIdIn(miniatureIds)
                    .forEach(row -> likeCountMap.put((Long) row[0], (Long) row[1]));
        }

        // 사용자가 좋아요한 미니어처 목록 일괄 조회
        Set<Long> likedMiniatureIds = new HashSet<>();
        if (userId != null && !miniatureIds.isEmpty()) {
            likedMiniatureIds.addAll(
                    miniatureLikeRepository.findMiniatureIdsByUserIdAndMiniatureIdIn(userId, miniatureIds)
            );
        }

        // 미니어처별 최신 공개 썸네일 일괄 조회
        Map<Long, String> thumbnailMap = buildThumbnailMap(miniatureIds, true);

        // Response 변환
        Page<PublicMiniatureResponse> responsePage = miniatures.map(miniature -> {
            int progress = calculateProgress(miniature.getId());
            String nickname = userNicknames.getOrDefault(miniature.getUserId(), "");
            long likeCount = likeCountMap.getOrDefault(miniature.getId(), 0L);
            boolean liked = likedMiniatureIds.contains(miniature.getId());
            String thumbnailUrl = thumbnailMap.get(miniature.getId());
            return PublicMiniatureResponse.of(miniature, progress, nickname, likeCount, liked, thumbnailUrl);
        });

        return PublicMiniaturePageResponse.from(responsePage);
    }

    /**
     * 공개 미니어처 상세 조회 (좋아요 정보 포함)
     * @param userId 현재 로그인 사용자 ID (null이면 비로그인)
     */
    public PublicMiniatureDetailResponse getPublicMiniatureDetail(Long miniatureId, Long userId) {
        // 1. 공개 미니어처 조회
        Miniature miniature = miniatureRepository.findByIdAndIsPublicTrue(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        // 2. 사용자 닉네임 조회
        User user = userRepository.findById(miniature.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. BacklogItem 조회
        List<BacklogItem> backlogItems = backlogItemRepository
                .findByMiniatureIdOrderByOrderIndexAsc(miniatureId);

        List<BacklogItemResponse> backlogItemResponses = backlogItems.stream()
                .map(BacklogItemResponse::from)
                .toList();

        // 4. 진행률 계산
        int progress = calculateProgress(miniatureId);

        // 5. 좋아요 정보 조회
        long likeCount = miniatureLikeRepository.countByMiniatureId(miniatureId);
        boolean liked = userId != null && miniatureLikeRepository.existsByUserIdAndMiniatureId(userId, miniatureId);

        return PublicMiniatureDetailResponse.of(miniature, progress, user.getNickname(), backlogItemResponses, likeCount, liked);
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
     * 단계 일괄 변경 (칸반 드래그)
     */
    @Transactional
    public MiniatureResponse updateCurrentStep(Long miniatureId, Long userId, MiniatureStepUpdateRequest request) {
        // 1. Miniature 조회 + 소유권 검증
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));
        validateOwnership(miniature, userId);

        // 2. 모든 backlogItem 조회
        List<BacklogItem> items = backlogItemRepository
                .findByMiniatureIdOrderByOrderIndexAsc(miniatureId);

        // 3. 단계에 따라 일괄 상태 변경
        String targetStep = request.getCurrentStep();
        if ("시작전".equals(targetStep)) {
            // 전부 TODO
            items.forEach(item -> item.updateStatus(BacklogItemStatus.TODO));
        } else if ("완료".equals(targetStep)) {
            // 전부 DONE
            items.forEach(item -> item.updateStatus(BacklogItemStatus.DONE));
        } else {
            // 해당 step 이하 DONE, 이후 TODO
            int targetIndex = -1;
            for (BacklogItem item : items) {
                if (item.getStepName().equals(targetStep)) {
                    targetIndex = item.getOrderIndex();
                    break;
                }
            }
            if (targetIndex < 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            for (BacklogItem item : items) {
                if (item.getOrderIndex() <= targetIndex) {
                    item.updateStatus(BacklogItemStatus.DONE);
                } else {
                    item.updateStatus(BacklogItemStatus.TODO);
                }
            }
        }

        log.info("미니어처 단계 일괄 변경: miniatureId={}, targetStep={}", miniatureId, targetStep);

        // 4. Response 생성
        int progress = calculateProgressFromItems(items);
        String currentStep = calculateCurrentStep(items);
        return MiniatureResponse.of(miniature, progress, currentStep, null);
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
     * 아이템 목록에서 진행률 계산
     */
    private int calculateProgressFromItems(List<BacklogItem> items) {
        if (items.isEmpty()) {
            return 0;
        }
        long done = items.stream()
                .filter(item -> item.getStatus() == BacklogItemStatus.DONE)
                .count();
        return (int) Math.round((double) done / items.size() * 100);
    }

    /**
     * 현재 단계 계산 (연속 DONE 기반)
     * - 0개 DONE → "시작전"
     * - 모두 DONE → "완료"
     * - N개 연속 DONE → N번째 step의 stepName
     */
    private String calculateCurrentStep(List<BacklogItem> items) {
        if (items.isEmpty()) {
            return "시작전";
        }

        // 처음부터 연속으로 DONE인 개수 세기
        int consecutiveDone = 0;
        for (BacklogItem item : items) {
            if (item.getStatus() == BacklogItemStatus.DONE) {
                consecutiveDone++;
            } else {
                break;
            }
        }

        if (consecutiveDone == 0) {
            return "시작전";
        }
        if (consecutiveDone == items.size()) {
            return "완료";
        }
        // 연속 DONE 마지막 아이템의 stepName 반환
        return items.get(consecutiveDone - 1).getStepName();
    }

    /**
     * 미니어처별 최신 썸네일 URL 맵 생성
     * @param miniatureIds 미니어처 ID 목록
     * @param publicOnly true: 공개 로그의 이미지만, false: 모든 이미지
     */
    private Map<Long, String> buildThumbnailMap(List<Long> miniatureIds, boolean publicOnly) {
        if (miniatureIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = publicOnly
                ? imageRepository.findLatestPublicImageByMiniatureIds(miniatureIds)
                : imageRepository.findLatestImageByMiniatureIds(miniatureIds);

        Map<Long, String> thumbnailMap = new HashMap<>();
        for (Object[] row : rows) {
            Long miniatureId = ((Number) row[0]).longValue();
            String objectKey = (String) row[1];
            String url = publicOnly
                    ? imageService.generatePublicUrl(objectKey)
                    : imageService.generateReadPresignedUrl(objectKey);
            thumbnailMap.put(miniatureId, url);
        }
        return thumbnailMap;
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
