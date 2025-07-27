package com.hyetaekon.hyetaekon.publicservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PublicServiceListResponseDto {
    private String publicServiceId;
    private String serviceName;
    private String summaryPurpose;

    // 해시태그
    private String serviceCategory;  // 서비스 분야
    private List<String> specialGroup;  // 특수 대상 그룹
    private List<String> familyType;  // 가구 형태

    private boolean bookmarked;  // 북마크 여부
}
