package com.hyetaekon.hyetaekon.publicservice.converter;

import com.hyetaekon.hyetaekon.common.converter.GenericCodeConverter;

import com.hyetaekon.hyetaekon.publicservice.entity.FamilyTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FamilyTypeConverter extends GenericCodeConverter<FamilyTypeEnum> {
    public FamilyTypeConverter() {
        super(FamilyTypeEnum.class);
    }
}
