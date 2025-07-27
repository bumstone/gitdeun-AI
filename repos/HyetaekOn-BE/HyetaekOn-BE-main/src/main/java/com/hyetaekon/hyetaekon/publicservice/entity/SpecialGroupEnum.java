package com.hyetaekon.hyetaekon.publicservice.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SpecialGroupEnum implements CodeEnum {
    IS_MULTI_CULTURAL("다문화가족", "JA0401"),
    IS_NORTH_KOREAN_DEFECTOR("북한이탈주민", "JA0402"),
    IS_SINGLE_PARENT_FAMILY("한부모가정/조손가정", "JA0403"),
    IS_SINGLE_MEMBER_HOUSEHOLD("1인가구", "JA0404"),
    IS_DISABLED("장애인", "JA0328"),
    IS_NATIONAL_MERIT_RECIPIENT("국가보훈대상자", "JA0329"),
    IS_CHRONIC_ILLNESS("질병/질환자", "JA0330");

    @JsonValue
    private final String type;
    private final String code;
}