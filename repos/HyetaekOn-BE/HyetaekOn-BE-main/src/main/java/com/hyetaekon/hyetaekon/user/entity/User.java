package com.hyetaekon.hyetaekon.user.entity;

import com.hyetaekon.hyetaekon.userInterest.entity.UserInterest;
import com.hyetaekon.hyetaekon.bookmark.entity.Bookmark;
import com.hyetaekon.hyetaekon.recommend.entity.Recommend;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user", uniqueConstraints = {
    @UniqueConstraint(name = "up_user_real_id_deleted_at", columnNames = {"real_id", "deleted_at"}),
}, indexes = {
    @Index(name = "idx_user_real_id_deleted_at", columnList = "real_id, deletedAt"),
    @Index(name = "idx_user_nickname_deleted_at", columnList = "nickname, deletedAt"),
    @Index(name = "idx_user_deleted_at_suspend_end_at", columnList = "deletedAt, suspendEndAt"),
    @Index(name = "idx_user_created_at", columnList = "createdAt DESC")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "real_id", nullable = false, length = 100)
    private String realId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "birth_at")
    private LocalDate birthAt;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    // TODO: BusinessType, Occupation 고려
    @Column(name = "job", length = 50)
    private String job;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 50)
    private UserLevel level;

    // TODO: 게시글 작성, 댓글 및 질문글 답변 시 적용
    @Column(name = "point")
    private int point;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;

    @Column(name = "suspend_start_at")
    private LocalDateTime suspendStartAt;

    @Column(name = "suspend_end_at")
    private LocalDateTime suspendEndAt;

    @Column(name = "suspend_reason", columnDefinition = "TEXT")
    private String suspendReason;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recommend> recommends = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserInterest> interests = new ArrayList<>();

    // 회원 탈퇴 로직
    public void deleteUser(String deleteReason) {
        this.deletedAt = LocalDateTime.now();
        this.deleteReason = deleteReason;
    }

    // 회원 닉네임 변경
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }


    // 회원 비밀번호 변경
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    // 회원 이름 변경
    public void updateName(String newName) {
        this.name = newName;
    }

    // 회원 생년월일 변경
    public void updateBirthAt(LocalDate newBirthAt) {
        this.birthAt = newBirthAt;
    }

    // 회원 성별 변경
    public void updateGender(String newGender) {
        this.gender = newGender;
    }

    // 회원 지역(시/도) 변경
    public void updateCity(String newCity) {
        this.city = newCity;
    }

    // 회원 지역(시/군/구) 변경
    public void updateState(String newState) {
        this.state = newState;
    }

    // 회원 직업 정보 변경
    public void updateJob(String newJob) {
        this.job = newJob;
    }

    // 회원 등급 Enum 변경
    public void updateLevel(UserLevel level) {
        this.level = level;
    }

    // 점수 추가
    public void addPoint(int amount) {
        this.point += amount;
    }

    // 점수 감점 (0점 이상으로)
    public void subtractPoint(int amount) {
        this.point = Math.max(0, this.point - amount);
    }

}
