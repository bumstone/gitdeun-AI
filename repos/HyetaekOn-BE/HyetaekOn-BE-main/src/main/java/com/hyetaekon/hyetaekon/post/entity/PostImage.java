package com.hyetaekon.hyetaekon.post.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "post_image", indexes = {
    @Index(name = "idx_post_image_post_id_deleted_at", columnList = "post_id, deletedAt")
})
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    private LocalDateTime deletedAt;

    // Soft delete 처리 메소드
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
