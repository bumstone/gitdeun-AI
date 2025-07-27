package com.hyetaekon.hyetaekon.common.publicdata.mapper;

import com.hyetaekon.hyetaekon.common.converter.ServiceCategoryConverter;
import com.hyetaekon.hyetaekon.common.publicdata.dto.*;
import com.hyetaekon.hyetaekon.publicservice.converter.BusinessTypeConverter;
import com.hyetaekon.hyetaekon.publicservice.converter.FamilyTypeConverter;
import com.hyetaekon.hyetaekon.publicservice.converter.OccupationConverter;
import com.hyetaekon.hyetaekon.publicservice.converter.SpecialGroupConverter;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
    uses = {
        ServiceCategoryConverter.class,
        SpecialGroupConverter.class,
        FamilyTypeConverter.class,
        OccupationConverter.class,
        BusinessTypeConverter.class
    },
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicServiceDataMapper {

    @Mapping(target = "id", source = "serviceId")
    PublicService updateFromServiceData(@MappingTarget PublicService publicService, PublicServiceDataDto.Data data);

    // 공공서비스 상세정보 데이터 매핑
    PublicService updateFromDetailData(@MappingTarget PublicService publicService, PublicServiceDetailDataDto.Data data);

    // 공공서비스 지원조건 데이터 매핑
    @Mapping(target = "targetGenderMale", source = "targetGenderMale")
    @Mapping(target = "targetGenderFemale", source = "targetGenderFemale")
    @Mapping(target = "incomeLevel", expression = "java(mapIncomeLevel(data))")
    PublicService updateFromConditionsData(@MappingTarget PublicService publicService, PublicServiceConditionsDataDto.Data data);

    // 소득 수준을 문자열로 매핑
    default String mapIncomeLevel(PublicServiceConditionsDataDto.Data data) {
        StringBuilder levels = new StringBuilder();

        if ("Y".equals(data.getIncomeLevelVeryLow())) levels.append("0-50%, ");
        if ("Y".equals(data.getIncomeLevelLow())) levels.append("51-75%, ");
        if ("Y".equals(data.getIncomeLevelMedium())) levels.append("76-100%, ");
        if ("Y".equals(data.getIncomeLevelHigh())) levels.append("101-200%, ");
        if ("Y".equals(data.getIncomeLevelVeryHigh())) levels.append("200%+, ");

        if (!levels.isEmpty()) {
            return levels.substring(0, levels.length() - 2); // 마지막 ", " 제거
        }

        return null;
    }
}