package com.hyetaekon.hyetaekon.publicservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PublicServiceDetailResponseDto {
    private String publicServiceId;
    private String serviceName;
    private String servicePurpose;  // 서비스 목적

    private int views;
    private int bookmarkCnt;
    private boolean bookmarked;   // 북마크 여부

    // 담당부서
    // TODO: 지역정보
    private String governingAgency;  // 소관기관명
    private String contactInfo;  // 문의처

    // 지원대상
    private String supportTarget;  // 지원 대상
    private String selectionCriteria;  // 선정 기준

    // 지원내용
    private String supportDetail;  // 지원 내용
    private String supportType;  // 지원 유형

    // 신청방법
    private String applicationMethod;  // 신청 방법
    private String applicationDeadline;  // 신청 기한
    // private String requiredDocuments;  // 구비 서류

    // 추가 정보
    private String onlineApplicationUrl;  // 온라인 경로 url
    // private String relatedLaws;  // 관련 법률

}
