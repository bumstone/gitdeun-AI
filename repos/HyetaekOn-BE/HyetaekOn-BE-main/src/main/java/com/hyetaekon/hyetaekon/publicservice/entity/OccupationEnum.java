package com.hyetaekon.hyetaekon.publicservice.entity;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OccupationEnum implements CodeEnum {
    IS_FARMER("농업인", "JA0313"),
    IS_FISHERMAN("어업인", "JA0314"),
    IS_STOCK_BREEDER("축산업인", "JA0315"),
    IS_FORESTER("임업인", "JA0316"),
    IS_ELEMENTARY_STUDENT("초등학생", "JA0317"),
    IS_MIDDLE_SCHOOL_STUDENT("중학생", "JA0318"),
    IS_HIGH_SCHOOL_STUDENT("고등학생", "JA0319"),
    IS_UNIVERSITY_STUDENT("대학생/대학원생", "JA0320"),
    IS_WORKER("근로자/직장인", "JA0326"),
    IS_JOB_SEEKER("구직자/실업자", "JA0327");

    @JsonValue
    private final String type;
    private final String code;
}
