package com.hyetaekon.hyetaekon.user.controller;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.common.jwt.JwtToken;
import com.hyetaekon.hyetaekon.common.util.CookieUtil;
import com.hyetaekon.hyetaekon.user.dto.UserSignInRequestDto;
import com.hyetaekon.hyetaekon.user.dto.UserTokenResponseDto;
import com.hyetaekon.hyetaekon.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    // 로그인 처리
    @PostMapping("/login")
    public ResponseEntity<UserTokenResponseDto> login(@RequestBody UserSignInRequestDto userSignInRequestDto,
                                                      HttpServletResponse response) {

        JwtToken jwtToken = authService.login(userSignInRequestDto);
        cookieUtil.setCookie(response, "refreshToken", jwtToken.getRefreshToken(),refreshTokenExpired);

        return ResponseEntity.ok(new UserTokenResponseDto(jwtToken.getAccessToken()));
    }

    // 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestHeader("Authorization") String authHeader,
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        // 헤더에서 Access Token 추출
        String accessToken = authHeader.replace("Bearer ", "");

        // 로그아웃 로직 - AccessToken: Blacklist 등록, RefreshToken: redis에서 삭제 및 쿠키 제거
        authService.logout(accessToken, refreshToken, response);

        return ResponseEntity.noContent().build(); // 204 No Content 응답
    }
    
    // 토큰 재발급
    @GetMapping("/token/refresh")
    public ResponseEntity<UserTokenResponseDto> refreshAccessToken(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response) {

        if (refreshToken == null) {
            throw new GlobalException(ErrorCode.NO_TOKEN);
        }

        JwtToken jwtToken = authService.refresh(refreshToken);
        cookieUtil.setCookie(response, "refreshToken", jwtToken.getRefreshToken(), refreshTokenExpired);

        // JWT 토큰 정보 반환
        return ResponseEntity.ok(new UserTokenResponseDto(jwtToken.getAccessToken()));
    }



}
