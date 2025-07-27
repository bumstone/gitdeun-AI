package com.hyetaekon.hyetaekon.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 개인정보수정 - 그 외 정보 변경 항목
@Getter
@Builder
public class UserProfileUpdateDto {
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,8}$", message = "닉네임은 알파벳, 숫자, 한글만 포함할 수 있습니다.")
    private String nickname;

    private String name; // 이름
    private LocalDate birthAt; // 생년월일
    private String gender; // 성별(남자/여자)
    private String city; // 지역(시/도)
    private String state;  // 지역(시/군/구)
    private String job; // 직업
}
