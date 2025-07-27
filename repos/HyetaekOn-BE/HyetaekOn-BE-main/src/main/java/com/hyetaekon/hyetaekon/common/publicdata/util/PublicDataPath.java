package com.hyetaekon.hyetaekon.common.publicdata.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PublicDataPath {
    SERVICE_LIST("/serviceList"),
    SERVICE_DETAIL("/serviceDetail"),
    SERVICE_CONDITIONS("/supportConditions");

    private final String path;
}
