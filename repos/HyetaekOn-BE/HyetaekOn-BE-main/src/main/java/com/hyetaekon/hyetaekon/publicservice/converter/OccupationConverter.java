package com.hyetaekon.hyetaekon.publicservice.converter;

import com.hyetaekon.hyetaekon.common.converter.GenericCodeConverter;

import com.hyetaekon.hyetaekon.publicservice.entity.OccupationEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OccupationConverter extends GenericCodeConverter<OccupationEnum> {
    public OccupationConverter() {
        super(OccupationEnum.class);
    }
}
