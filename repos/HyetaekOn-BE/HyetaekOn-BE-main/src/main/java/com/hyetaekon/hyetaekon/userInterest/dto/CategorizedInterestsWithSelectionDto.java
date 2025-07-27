package com.hyetaekon.hyetaekon.userInterest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class CategorizedInterestsWithSelectionDto {
    private Map<String, List<InterestItemDto>> categorizedInterests;
}
