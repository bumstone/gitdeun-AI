package com.hyetaekon.hyetaekon.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_report",
    indexes = {
        @Index(name = "idx_user_report_status_created_at", columnList = "status, createdAt DESC"),
        @Index(name = "idx_user_report_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_user_report_reported_id", columnList = "reported_id")
    }
)
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 신고한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    // 신고당한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    private User reported;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}