package com.hyetaekon.hyetaekon.publicservice.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FamilyTypeEnum implements CodeEnum {
    // IS_NOT_APPLICABLE("해당사항 없음", "JA0410"),
    IS_MULTI_CHILDREN_FAMILY("다자녀가구", "JA0411"),
    IS_NON_HOUSING_HOUSEHOLD("무주택세대", "JA0412"),
    IS_NEW_RESIDENCE("신규전입", "JA0413"),
    IS_EXTENDED_FAMILY("확대가족", "JA0414");

    @JsonValue
    private final String type;
    private final String code;
}
