package com.hyetaekon.hyetaekon.publicservice.converter;


import com.hyetaekon.hyetaekon.common.converter.GenericCodeConverter;
import com.hyetaekon.hyetaekon.publicservice.entity.BusinessTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BusinessTypeConverter extends GenericCodeConverter<BusinessTypeEnum> {
    public BusinessTypeConverter() {
        super(BusinessTypeEnum.class);
    }
}
