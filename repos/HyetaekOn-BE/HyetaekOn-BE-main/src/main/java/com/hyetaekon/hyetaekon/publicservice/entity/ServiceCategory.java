package com.hyetaekon.hyetaekon.publicservice.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ServiceCategory {
    CHILDCARE_EDUCATION("보육·교육"),
    HOUSING_INDEPENDENCE("주거·자립"),
    ADMINISTRATION_SAFETY("행정·안전"),
    AGRICULTURE_FISHERY("농림축산어업"),
    EMPLOYMENT_STARTUP("고용·창업"),
    HEALTH_MEDICAL("보건·의료"),
    CULTURE_ENVIRONMENT("문화·환경"),
    LIFE_STABILITY("생활안정"),
    PROTECTION_CARE("보호·돌봄"),
    PREGNANCY_CHILDBIRTH("임신·출산"),
    OTHER("기타");


    @JsonValue
    private final String type;
}
