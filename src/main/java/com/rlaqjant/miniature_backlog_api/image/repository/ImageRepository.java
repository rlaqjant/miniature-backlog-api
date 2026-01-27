package com.rlaqjant.miniature_backlog_api.image.repository;

import com.rlaqjant.miniature_backlog_api.image.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
