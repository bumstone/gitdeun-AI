package com.hyetaekon.hyetaekon.userInterest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InterestItemDto {
    private String name;      // 관심사 이름
    private boolean selected; // 선택 여부
}
