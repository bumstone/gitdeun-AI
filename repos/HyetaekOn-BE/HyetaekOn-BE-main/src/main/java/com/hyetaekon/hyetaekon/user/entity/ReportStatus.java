package com.hyetaekon.hyetaekon.user.entity;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("처리 대기중"),
    RESOLVED("처리 완료"),
    REJECTED("거부됨");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

}
