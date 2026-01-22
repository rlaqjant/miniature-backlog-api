package com.rlaqjant.miniature_backlog_api.image.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이미지 엔티티
 * ProgressLog에 첨부된 이미지 메타데이터
 */
@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "progress_log_id", nullable = false)
    private Long progressLogId;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    // PRD 스펙에 따라 fileName, contentType은 선택적 필드로 변경
    // (향후 objectKey에서 추출 가능)
    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
