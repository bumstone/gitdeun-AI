package com.hyetaekon.hyetaekon.banner.mapper;

import com.hyetaekon.hyetaekon.banner.dto.BannerDto;
import com.hyetaekon.hyetaekon.banner.entity.Banner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BannerMapper {
    BannerDto toDto(Banner banner);

    @Mapping(target = "id", ignore = true)
    Banner toEntity(BannerDto bannerDto);
}
