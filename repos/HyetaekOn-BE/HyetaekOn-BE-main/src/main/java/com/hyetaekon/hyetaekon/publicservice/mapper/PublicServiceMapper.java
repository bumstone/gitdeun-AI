package com.hyetaekon.hyetaekon.publicservice.mapper;

import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceDetailResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


// 엔티티와 DTO 간 매핑 시 매핑되지 않은 필드가 있어도 MapStruct가 경고나 오류를 생성 x
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicServiceMapper {
    @Mapping(source = "id", target = "publicServiceId")
    @Mapping(target = "serviceCategory", expression = "java(publicService.getServiceCategory().getType())")
    @Mapping(target = "specialGroup", expression = "java(publicService.getSpecialGroups().stream().map(sg -> sg.getSpecialGroupEnum().getType()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "familyType", expression = "java(publicService.getFamilyTypes().stream().map(ft -> ft.getFamilyTypeEnum().getType()).collect(java.util.stream.Collectors.toList()))")
    PublicServiceListResponseDto toListDto(PublicService publicService);

    @Mapping(source = "id", target = "publicServiceId")
    PublicServiceDetailResponseDto toDetailDto(PublicService publicService);
}
