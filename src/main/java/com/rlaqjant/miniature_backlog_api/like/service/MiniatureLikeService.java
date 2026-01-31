package com.rlaqjant.miniature_backlog_api.like.service;

import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.like.domain.MiniatureLike;
import com.rlaqjant.miniature_backlog_api.like.dto.LikeResponse;
import com.rlaqjant.miniature_backlog_api.like.repository.MiniatureLikeRepository;
import com.rlaqjant.miniature_backlog_api.miniature.domain.Miniature;
import com.rlaqjant.miniature_backlog_api.miniature.repository.MiniatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 미니어처 좋아요 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MiniatureLikeService {

    private final MiniatureLikeRepository miniatureLikeRepository;
    private final MiniatureRepository miniatureRepository;

    /**
     * 좋아요 토글 (있으면 삭제, 없으면 생성)
     */
    @Transactional
    public LikeResponse toggleLike(Long userId, Long miniatureId) {
        // 공개 미니어처 여부 검증
        Miniature miniature = miniatureRepository.findByIdAndIsPublicTrue(miniatureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_PUBLIC_MINIATURE));

        Optional<MiniatureLike> existingLike = miniatureLikeRepository
                .findByUserIdAndMiniatureId(userId, miniatureId);

        boolean liked;
        if (existingLike.isPresent()) {
            // 좋아요 취소
            miniatureLikeRepository.delete(existingLike.get());
            liked = false;
            log.info("좋아요 취소: userId={}, miniatureId={}", userId, miniatureId);
        } else {
            // 좋아요 추가
            MiniatureLike newLike = MiniatureLike.builder()
                    .userId(userId)
                    .miniatureId(miniatureId)
                    .build();
            miniatureLikeRepository.save(newLike);
            liked = true;
            log.info("좋아요 추가: userId={}, miniatureId={}", userId, miniatureId);
        }

        long likeCount = miniatureLikeRepository.countByMiniatureId(miniatureId);

        return LikeResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    /**
     * 좋아요 수 조회
     */
    public long getLikeCount(Long miniatureId) {
        return miniatureLikeRepository.countByMiniatureId(miniatureId);
    }

    /**
     * 사용자의 좋아요 여부 조회
     */
    public boolean isLikedByUser(Long userId, Long miniatureId) {
        return miniatureLikeRepository.existsByUserIdAndMiniatureId(userId, miniatureId);
    }

    /**
     * 미니어처 삭제 시 좋아요 연쇄 삭제
     */
    @Transactional
    public void deleteByMiniatureId(Long miniatureId) {
        miniatureLikeRepository.deleteByMiniatureId(miniatureId);
    }
}
