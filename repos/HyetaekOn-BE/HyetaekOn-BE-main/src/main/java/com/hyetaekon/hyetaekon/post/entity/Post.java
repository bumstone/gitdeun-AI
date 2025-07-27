package com.hyetaekon.hyetaekon.post.entity;

import com.hyetaekon.hyetaekon.bookmark.entity.Bookmark;
import com.hyetaekon.hyetaekon.common.util.BaseEntity;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.recommend.entity.Recommend;
import com.hyetaekon.hyetaekon.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post", indexes = {
    // 가장 빈번한 정렬/필터 조합 고려
    @Index(name = "idx_post_deleted_at_created_at", columnList = "deletedAt, createdAt DESC"),
    // 타입별 조회 및 정렬
    @Index(name = "idx_post_deleted_at_post_type_created_at", columnList = "deletedAt, postType, createdAt DESC"),
    // 사용자별 게시글 조회 및 정렬
    @Index(name = "idx_post_user_id_deleted_at_created_at", columnList = "user_id, deletedAt, createdAt DESC"),
    @Index(name = "idx_post_deleted_at_title", columnList = "deletedAt, title"), // 좋아용한 게시글
    @Index(name = "idx_post_suspend_at_created_at", columnList = "suspendAt, createdAt DESC") // 정지된 게시글
})
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 게시글 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_service_id")
    private PublicService publicService;

    @Column(columnDefinition = "VARCHAR(40) CHARACTER SET utf8mb4", nullable = false)  // ✅ 제목 20자 제한
    private String title;

    @Column(columnDefinition = "VARCHAR(500) CHARACTER SET utf8mb4", nullable = false)  // ✅ 내용 500자 제한
    private String content;

    @Builder.Default
    @Column(name = "recommend_cnt")
    private int recommendCnt = 0;  // 추천수

    @Builder.Default
    @Column(name = "view_count")
    private int viewCnt = 0;  // 조회수

    // TODO: 댓글 생성/수정 시 업데이트
    @Builder.Default
    @Column(name = "comment_cnt")
    private int commentCnt = 0;   // 댓글수

    @Column(name = "post_type", nullable = false)
    @Enumerated(EnumType.STRING)  // ✅ ENUM 타입으로 저장 (질문, 자유, 인사)
    private PostType postType;

    private String serviceUrl;

    @Column(columnDefinition = "VARCHAR(12) CHARACTER SET utf8mb4")  // ✅ url제목 12자 제한
    private String urlTitle;

    private String urlPath;

    @Column(length = 255)
    private String tags; // ✅ 태그는 최대 3개 (쉼표 구분)

    @Column(name = "category_id")
    private Long categoryId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "suspend_at")
    private LocalDateTime suspendAt;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> postImages = new ArrayList<>();  // ✅ 게시글 이미지와 연결

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recommend> recommends = new ArrayList<>();

    // 조회수 증가
    public void incrementViewCnt() {
        this.viewCnt++;
    }

    // 추천수 증가
    public void incrementRecommendCnt() {
        this.recommendCnt++;
    }

    // 추천수 감소
    public void decrementRecommendCnt() {
        this.recommendCnt = Math.max(0, this.recommendCnt - 1);
    }

    public void incrementCommentCnt() {
        this.commentCnt++;
    }

    public void decrementCommentCnt() {
        this.commentCnt = Math.max(0, this.commentCnt - 1);
    }

    // 삭제 처리
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 정지 처리
    public void suspend() {
        this.suspendAt = LocalDateTime.now();
    }

    public String getDisplayTitle() {
        if (this.deletedAt != null) {
            return "사용자가 삭제한 게시글입니다.";
        } else if (this.suspendAt != null) {
            return "관리자에 의해 삭제된 게시글입니다.";
        }
        return title;
    }

    public String getDisplayContent() {
        if (this.deletedAt != null) {
            return "사용자가 삭제한 게시글입니다.";
        } else if (this.suspendAt != null) {
            return "관리자에 의해 삭제된 게시글입니다.";
        }
        return content;
    }

}
