package com.hyetaekon.hyetaekon.user.mapper;

import com.hyetaekon.hyetaekon.user.dto.UserResponseDto;
import com.hyetaekon.hyetaekon.user.dto.UserSignUpResponseDto;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.entity.UserLevel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    /*// 회원가입 요청 DTO -> User Entity 변환
    @Mapping(target = "role", constant = "ROLE_USER") // 기본 Role 설정
    User toEntity(UserSignUpRequestDto dto);*/

    // User Entity -> 회원가입 응답 DTO 변환
    UserSignUpResponseDto toSignUpResponseDto(User user);

    // User Entity -> 회원 정보 조회 DTO 변환
    @Mapping(target = "levelName", expression = "java(user.getLevel().getName())")
    @Mapping(target = "remainPoint", expression = "java(calculateRemainPoint(user))")
    UserResponseDto toResponseDto(User user);

    default int calculateRemainPoint(User user) {
        UserLevel currentLevel = user.getLevel();
        if (currentLevel == UserLevel.CLOUD) {
            // 이미 최고 레벨인 경우
            return 0;
        }

        // 현재 레벨의 최대 포인트와 사용자 현재 포인트의 차이를 계산
        return currentLevel.getMaxPoint() - user.getPoint() + 1;
    }
}
