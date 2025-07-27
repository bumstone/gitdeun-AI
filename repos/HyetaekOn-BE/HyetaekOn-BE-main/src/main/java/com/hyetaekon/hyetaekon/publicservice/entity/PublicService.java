package com.hyetaekon.hyetaekon.publicservice.entity;

import com.hyetaekon.hyetaekon.bookmark.entity.Bookmark;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "public_service", indexes = {
    @Index(name = "idx_publicservice_bookmark_cnt", columnList = "bookmark_cnt DESC"),
    @Index(name = "idx_publicservice_service_category", columnList = "service_category"),
    @Index(name = "idx_publicservice_views", columnList = "views")
})
public class PublicService {
    @Id
    private String id;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;  // 서비스명

    // 서비스 분야 - 카테고리 + 해시태그
    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false)
    private ServiceCategory serviceCategory;   // 서비스 분야

    @Column(name = "summary_purpose", columnDefinition = "TEXT")
    private String summaryPurpose;  // 서비스 목적 요약

    @Column(name = "governing_agency", length = 100)
    private String governingAgency;  // 소관기관명

    @Column(name = "department", length = 100)
    private String department;  // 부서명

    @Column(name = "user_type", length = 50)
    private String userType;  // 사용자 구분


    // 지원 대상 필드
    @Column(name = "support_target", columnDefinition = "TEXT")
    private String supportTarget;  // 지원 대상

    @Column(name = "selection_criteria", columnDefinition = "TEXT")
    private String selectionCriteria;  // 선정 기준


    // 지원 관련 필드
    @Column(name = "service_purpose", columnDefinition = "TEXT")
    private String servicePurpose;  // 서비스 목적

    @Column(name = "support_detail", columnDefinition = "TEXT")
    private String supportDetail;  // 지원 내용

    @Column(name = "support_type", length = 100)
    private String supportType;  // 지원 유형


    // 신청 내용 필드
    @Column(name = "application_method", columnDefinition = "TEXT")
    private String applicationMethod;  // 신청 방법(상세)

    @Column(name = "application_deadline", columnDefinition = "TEXT")
    private String applicationDeadline;  // 신청 기한(상세)


    // 추가정보 필드
//    @Column(name = "required_documents", columnDefinition = "TEXT")
//    private String requiredDocuments;  // 구비 서류

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;  // 문의처

    @Column(name = "online_application_url", columnDefinition = "TEXT")
    private String onlineApplicationUrl;  // 온라인 경로 url

//    @Column(name = "related_laws", columnDefinition = "TEXT")
//    private String relatedLaws;  // 관련 법률


    // 지원조건 필드 - 유저 정보 비교용
    @Column(name = "target_gender_male")
    private String targetGenderMale;

    @Column(name = "target_gender_female")
    private String targetGenderFemale;

    @Column(name = "target_age_start")
    private Integer targetAgeStart;

    @Column(name = "target_age_end")
    private Integer targetAgeEnd;

    @Column(name = "income_level", length = 255)
    private String incomeLevel;

    /*@Column(name = "income_level_very_low")
    private boolean incomeLevelVeryLow; // 중위소득 0~50%
    @Column(name = "income_level_low")
    private boolean incomeLevelLow; // 중위소득 51~75%
    @Column(name = "income_level_medium")
    private boolean incomeLevelMedium; // 중위소득 76~100%
    @Column(name = "income_level_high")
    private boolean incomeLevelHigh; // 중위소득 101~200%
    @Column(name = "income_level_very_high")
    private boolean incomeLevelVeryHigh; // 중위소득 200% 초과*/

    // 조회수
    @Builder.Default
    @Column(name = "views", nullable = false)
    private Integer views = 0;

    // 북마크수
    @Builder.Default
    @Column(name = "bookmark_cnt", nullable = false)
    private Integer bookmarkCnt = 0;


    @OneToMany(mappedBy = "publicService", cascade = {CascadeType.ALL},
        orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SpecialGroup> specialGroups = new ArrayList<>();

    @OneToMany(mappedBy = "publicService", cascade = {CascadeType.ALL},
        orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FamilyType> familyTypes = new ArrayList<>();

    @OneToMany(mappedBy = "publicService", cascade = {CascadeType.ALL},
        orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Occupation> occupations = new ArrayList<>();

    @OneToMany(mappedBy = "publicService", cascade = {CascadeType.ALL},
        orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BusinessType> businessTypes = new ArrayList<>();

    @OneToMany(mappedBy = "publicService", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bookmark> bookmarks = new ArrayList<>();

    public void increaseBookmarkCount() {
        bookmarkCnt++;
    }

    public void decreaseBookmarkCount() {
        bookmarkCnt--;
    }

    public void updateViewsUp() { views++; }

}
