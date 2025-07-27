package com.hyetaekon.hyetaekon.publicservice.entity.mongodb;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_info")  // 실제 몽고 DB 컬렉션 이름
public class ServiceInfo {
    private String publicServiceId;
    private String serviceName;
    private String summaryPurpose;

    // 해시태그
    private String serviceCategory;  // 서비스 분야
    private List<String> specialGroup;  // 특수 대상 그룹
    private List<String> familyType;  // 가구 형태

    private List<String> occupations;
    private List<String> businessTypes;

    // Support conditions fields
    private String targetGenderMale;
    private String targetGenderFemale;
    private Integer targetAgeStart;
    private Integer targetAgeEnd;
    private String incomeLevel;
}
