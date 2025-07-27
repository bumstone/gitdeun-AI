package com.hyetaekon.hyetaekon.user.mapper;

import com.hyetaekon.hyetaekon.user.dto.admin.UserAdminResponseDto;
import com.hyetaekon.hyetaekon.user.dto.admin.UserReportResponseDto;
import com.hyetaekon.hyetaekon.user.entity.UserReport;
import com.hyetaekon.hyetaekon.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserAdminMapper {

    // User Entity -> 관리자용 회원 정보 DTO 변환
    @Mapping(target = "levelName", expression = "java(user.getLevel().getName())")
    UserAdminResponseDto toAdminResponseDto(User user);

    // UserReport Entity -> 신고 내역 DTO 변환
    @Mapping(source = "reporter.nickname", target = "reporterNickname")
    @Mapping(source = "reported.nickname", target = "reportedNickname")
    @Mapping(source = "status.description", target = "status")
    UserReportResponseDto toReportResponseDto(UserReport userReport);
}
