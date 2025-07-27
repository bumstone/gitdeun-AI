package com.hyetaekon.hyetaekon.recommend.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hyetaekon.hyetaekon.common.util.BaseEntity;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recommend", indexes = {
    @Index(name = "idx_recommend_user_post", columnList = "user_id, post_id", unique = true), // 주요 조회 조건 및 중복 방지
    @Index(name = "idx_recommend_post_id", columnList = "post_id") // 게시글기준 좋아요 목록 조회
})
public class Recommend extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}