package com.rlaqjant.miniature_backlog_api.image.repository;

import com.rlaqjant.miniature_backlog_api.image.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 이미지 Repository
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 진행 로그 ID로 이미지 목록 조회
     */
    List<Image> findByProgressLogIdOrderByCreatedAtAsc(Long progressLogId);

    /**
     * 여러 진행 로그 ID로 이미지 목록 조회
     */
    List<Image> findByProgressLogIdIn(List<Long> progressLogIds);

    /**
     * 여러 진행 로그 ID로 이미지 일괄 삭제
     */
    void deleteByProgressLogIdIn(List<Long> progressLogIds);

    /**
     * 내 백로그용: 미니어처별 최신 이미지 objectKey 일괄 조회
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ON (pl.miniature_id)
                pl.miniature_id as miniatureId, i.object_key as objectKey
            FROM images i
            JOIN progress_logs pl ON pl.id = i.progress_log_id
            WHERE pl.miniature_id IN (:miniatureIds)
            ORDER BY pl.miniature_id, i.created_at DESC
            """)
    List<Object[]> findLatestImageByMiniatureIds(@Param("miniatureIds") List<Long> miniatureIds);

    /**
     * 공개 게시판용: 공개 진행 로그에서만 최신 이미지 objectKey 일괄 조회
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT ON (pl.miniature_id)
                pl.miniature_id as miniatureId, i.object_key as objectKey
            FROM images i
            JOIN progress_logs pl ON pl.id = i.progress_log_id
            WHERE pl.miniature_id IN (:miniatureIds) AND pl.is_public = true
            ORDER BY pl.miniature_id, i.created_at DESC
            """)
    List<Object[]> findLatestPublicImageByMiniatureIds(@Param("miniatureIds") List<Long> miniatureIds);
}
