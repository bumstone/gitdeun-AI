package com.hyetaekon.hyetaekon.answer.entity;

import com.hyetaekon.hyetaekon.common.util.BaseEntity;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "answer", indexes = {
        @Index(name = "idx_answer_post_id_selected_createdAt", columnList = "post_id, selected DESC, createdAt DESC"), // 주요 조회 및 정렬
        @Index(name = "idx_answer_post_id", columnList = "post_id"),
        @Index(name = "idx_answer_user_id", columnList = "user_id"),
        @Index(name = "idx_answer_post_id_deleted_at", columnList = "post_id, deletedAt"), // 삭제된 답변 조회
        @Index(name = "idx_answer_post_id_suspend_at", columnList = "post_id, suspendAt")  // 정지된 답변 조회
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 답변 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // 답변 내용

    @Column(name = "selected", nullable = false)
    private boolean selected; // 채택 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "suspend_at")
    private LocalDateTime suspendAt;

    // 삭제 처리
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 정지 처리
    public void suspend() {
        this.suspendAt = LocalDateTime.now();
    }

    public String getDisplayContent() {
        if (this.deletedAt != null) {
            return "사용자가 삭제한 답변입니다.";
        } else if (this.suspendAt != null) {
            return "관리자에 의해 삭제된 답변입니다.";
        }
        return content;
    }
}
