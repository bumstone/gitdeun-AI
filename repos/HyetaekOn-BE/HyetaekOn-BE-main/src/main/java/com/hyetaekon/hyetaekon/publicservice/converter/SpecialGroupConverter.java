package com.hyetaekon.hyetaekon.publicservice.converter;

import com.hyetaekon.hyetaekon.common.converter.GenericCodeConverter;

import com.hyetaekon.hyetaekon.publicservice.entity.SpecialGroupEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SpecialGroupConverter extends GenericCodeConverter<SpecialGroupEnum> {
    public SpecialGroupConverter() {
        super(SpecialGroupEnum.class);
    }
}