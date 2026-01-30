package com.rlaqjant.miniature_backlog_api.backlogitem.repository;

import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItem;
import com.rlaqjant.miniature_backlog_api.backlogitem.domain.BacklogItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 백로그 항목 Repository
 */
@Repository
public interface BacklogItemRepository extends JpaRepository<BacklogItem, Long> {

    /**
     * 미니어처 ID로 백로그 항목 목록 조회 (정렬된 순서)
     */
    List<BacklogItem> findByMiniatureIdOrderByOrderIndexAsc(Long miniatureId);

    /**
     * 미니어처 ID와 상태로 백로그 항목 개수 조회
     */
    long countByMiniatureIdAndStatus(Long miniatureId, BacklogItemStatus status);

    /**
     * 미니어처 ID로 백로그 항목 개수 조회
     */
    long countByMiniatureId(Long miniatureId);

    /**
     * 미니어처 ID 목록으로 백로그 항목 일괄 조회 (배치)
     */
    List<BacklogItem> findByMiniatureIdInOrderByMiniatureIdAscOrderIndexAsc(List<Long> miniatureIds);

    /**
     * 미니어처 ID로 백로그 항목 일괄 삭제
     */
    void deleteByMiniatureId(Long miniatureId);
}
