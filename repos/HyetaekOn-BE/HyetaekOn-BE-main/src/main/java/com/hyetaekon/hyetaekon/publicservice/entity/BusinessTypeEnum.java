package com.hyetaekon.hyetaekon.publicservice.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BusinessTypeEnum implements CodeEnum {
    IS_STARTUP_PREPARATION("예비창업자", "JA1101"),
    IS_BUSINESS_OPERATING("영업중", "JA1102"),
    IS_BUSINESS_HARDSHIP("생계곤란/폐업예정자", "JA1103"),
    IS_FOOD_INDUSTRY("음식업", "JA1201"),
    IS_MANUFACTURING_INDUSTRY("제조업", "JA1202"),
    IS_OTHER_INDUSTRY("기타업종", "JA1299"),
    IS_SMALL_MEDIUM_ENTERPRISE("중소기업", "JA2101"),
    IS_SOCIAL_WELFARE_INSTITUTION("사회복지시설", "JA2102"),
    IS_ORGANIZATION("기관/단체", "JA2103"),
    IS_MANUFACTURING_INDUSTRY_TYPE("제조업", "JA2201"),
    IS_AGRICULTURAL_INDUSTRY("농업, 임업 및 어업", "JA2202"),
    IS_INFORMATION_TECHNOLOGY_INDUSTRY("정보통신업", "JA2203"),
    IS_OTHER_INDUSTRY_TYPE("기타업종", "JA2299");

    @JsonValue
    private final String type;
    private final String code;
}
