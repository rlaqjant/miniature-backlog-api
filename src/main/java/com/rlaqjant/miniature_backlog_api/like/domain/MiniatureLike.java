package com.rlaqjant.miniature_backlog_api.like.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 미니어처 좋아요 엔티티
 */
@Entity
@Table(name = "miniature_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "miniature_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MiniatureLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "miniature_id", nullable = false)
    private Long miniatureId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
