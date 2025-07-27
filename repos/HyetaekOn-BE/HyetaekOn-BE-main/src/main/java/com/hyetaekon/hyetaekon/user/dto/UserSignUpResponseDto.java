package com.hyetaekon.hyetaekon.user.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class UserSignUpResponseDto {
    private Long id;       // 사용자 ID
    private String realId;  // 아이디
    private String nickname; // 닉네임
}