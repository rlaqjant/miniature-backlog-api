package com.rlaqjant.miniature_backlog_api.backlogitem.service;

import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItem;
import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemResponse;
import com.rlaqjant.miniature_backlog_api.backlogitem.dto.BacklogItemUpdateRequest;
import com.rlaqjant.miniature_backlog_api.backlogitem.repository.BacklogItemRepository;
import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백로그 항목 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BacklogItemService {

    private final BacklogItemRepository backlogItemRepository;
    private final MiniatureRepository miniatureRepository;

    /**
     * 백로그 항목 상태 변경
     */
    @Transactional
    public BacklogItemResponse updateStatus(Long backlogItemId, BacklogItemUpdateRequest request, Long userId) {
        // 1. BacklogItem 조회
        BacklogItem backlogItem = backlogItemRepository.findById(backlogItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BACKLOG_ITEM_NOT_FOUND));

        // 2. 소유권 검증 (BacklogItem → Miniature → userId)
        validateOwnership(backlogItem.getMiniatureId(), userId);

        // 3. 상태 변경
        backlogItem.updateStatus(request.getStatus());
        log.info("백로그 항목 상태 변경: id={}, status={}", backlogItemId, request.getStatus());

        // 4. 진행률 계산
        int progress = calculateProgress(backlogItem.getMiniatureId());

        return BacklogItemResponse.builder()
                .id(backlogItem.getId())
                .stepName(backlogItem.getStepName())
                .status(backlogItem.getStatus())
                .orderIndex(backlogItem.getOrderIndex())
                .progress(progress)
                .build();
    }

    /**
     * 소유권 검증
     */
    private void validateOwnership(Long miniatureId, Long userId) {
        Miniature miniature = miniatureRepository.findById(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MINIATURE_NOT_FOUND));

        if (!miniature.getUserId().equals(userId)) {
            log.warn("백로그 항목 접근 권한 없음: miniatureId={}, ownerId={}, requesterId={}",
                    miniatureId, miniature.getUserId(), userId);
            throw new BusinessException(ErrorCode.MINIATURE_ACCESS_DENIED);
        }
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
}
