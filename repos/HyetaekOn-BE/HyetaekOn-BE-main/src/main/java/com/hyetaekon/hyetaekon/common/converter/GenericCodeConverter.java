package com.hyetaekon.hyetaekon.common.converter;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.publicservice.entity.CodeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter(autoApply = true)
@RequiredArgsConstructor
public class GenericCodeConverter<E extends Enum<E> & CodeEnum> implements AttributeConverter<E, String> {

    private final Class<E> enumClass;

    // DB 저장: Enum -> 'code' 저장
    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute.getCode();
    }

    // DB 조회: 'code' -> Enum 조회
    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.getCode().equals(dbData)) {
                return enumConstant;
            }
        }
        throw new GlobalException(ErrorCode.INVALID_ENUM_CODE);
    }
}
