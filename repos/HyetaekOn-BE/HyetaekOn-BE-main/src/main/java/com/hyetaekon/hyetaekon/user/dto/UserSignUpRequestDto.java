package com.hyetaekon.hyetaekon.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignUpRequestDto {

    @NotBlank(message = "아이디는 공백일 수 없습니다.")
    private String realId;

    @NotBlank(message = "비밀번호는 공백일 수 없습니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
        message = "비밀번호는 8자 이상 20자 이하여야 하며, 알파벳, 숫자, 특수문자를 포함해야 합니다.")
    private String password; // 평문 비밀번호

    @NotBlank(message = "비밀번호 확인은 공백일 수 없습니다.")
    private String confirmPassword;

    @NotBlank(message = "이름은 공백일 수 없습니다.")
    private String name;

    @NotBlank(message = "닉네임은 공백일 수 없습니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,8}$",
        message = "닉네임은 1자 이상 8자 이하여야 하며, 알파벳, 숫자, 한글만 포함할 수 있습니다.")
    private String nickname;

    @NotNull(message = "생년월일은 공백일 수 없습니다.")
    private LocalDate birthAt;

    @NotBlank(message = "성별은 공백일 수 없습니다.")
    private String gender;

    @NotNull(message = "지역(시/도)은 공백일 수 없습니다.")
    private String city;
    @NotNull(message = "지역(시/군/구)은 공백일 수 없습니다.")
    private String state;

    private String job; // 직업
}
