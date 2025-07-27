package com.hyetaekon.hyetaekon.userInterest.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class InterestSelectionRequestDto {
    private Map<String, List<String>> categorizedInterests;

    public List<String> getAllInterests() {
        if (categorizedInterests == null) {
            return new ArrayList<>();
        }
        List<String> allInterests = new ArrayList<>();
        categorizedInterests.values().forEach(allInterests::addAll);
        return allInterests;
    }
}