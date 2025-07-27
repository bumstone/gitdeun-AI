package com.hyetaekon.hyetaekon.userInterest.entity;

import lombok.Getter;

@Getter
public enum UserInterestEnum {
    // ServiceCategory 관련 관심사
    CHILDCARE_EDUCATION("보육·교육", "관심주제"),
    HOUSING_INDEPENDENCE("주거·자립", "관심주제"),
    ADMINISTRATION_SAFETY("행정·안전", "관심주제"),
    AGRICULTURE_FISHERY("농림축산어업", "관심주제"),
    EMPLOYMENT_STARTUP("고용·창업", "관심주제"),
    HEALTH_MEDICAL("보건·의료", "관심주제"),
    CULTURE_ENVIRONMENT("문화·환경", "관심주제"),
    LIFE_STABILITY("생활안정", "관심주제"),
    PROTECTION_CARE("보호·돌봄", "관심주제"),  // 새로 추가된 카테고리
    OTHER("기타", "관심주제"),

    // SpecialGroup 관련 관심사
    IS_MULTI_CULTURAL("다문화가족", "가구형태"),
    IS_NORTH_KOREAN_DEFECTOR("북한이탈주민", "가구형태"),
    IS_SINGLE_PARENT_FAMILY("한부모가정/조손가정", "가구형태"),
    IS_SINGLE_MEMBER_HOUSEHOLD("1인가구", "가구형태"),
    IS_DISABLED("장애인", "가구형태"),
    IS_NATIONAL_MERIT_RECIPIENT("국가보훈대상자", "가구형태"),
    IS_CHRONIC_ILLNESS("질병/질환자", "가구형태"),

    // FamilyType 관련 관심사
    // IS_NOT_APPLICABLE("해당사항 없음", "가구상황"),
    IS_MULTI_CHILDREN_FAMILY("다자녀가구", "가구상황"),
    IS_NON_HOUSING_HOUSEHOLD("무주택세대", "가구상황"),
    IS_NEW_RESIDENCE("신규전입", "가구상황"),
    IS_EXTENDED_FAMILY("확대가족", "가구상황");

    private final String displayName;
    private final String category;

    UserInterestEnum(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }
}