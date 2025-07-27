package com.hyetaekon.hyetaekon.publicservice.mapper.mongodb;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.entity.mongodb.ServiceInfo;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceInfoMapper {

    @Mapping(source = "specialGroup", target = "specialGroup")
    @Mapping(source = "familyType", target = "familyType")
    PublicServiceListResponseDto toDto(ServiceInfo serviceInfo);
}
