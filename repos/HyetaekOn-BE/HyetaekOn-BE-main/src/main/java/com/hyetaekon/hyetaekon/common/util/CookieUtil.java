package com.hyetaekon.hyetaekon.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

@Component
public class CookieUtil {

  private final CookieProperties cookieProperties;

  public CookieUtil(CookieProperties cookieProperties) {
    this.cookieProperties = cookieProperties;
  }
  // 쿠키 설정
  public void setCookie(HttpServletResponse response, String name, String value, Long maxAge) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(cookieProperties.isSecure())
        .path("/")
        .sameSite(cookieProperties.getSameSite())
        .maxAge(maxAge)
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  // 쿠키 삭제
  public void deleteCookie(HttpServletResponse response, String name) {
    ResponseCookie cookie = ResponseCookie.from(name, null)
        .httpOnly(true)
        .secure(cookieProperties.isSecure())
        .path("/")
        .sameSite(cookieProperties.getSameSite())
        .maxAge(0) // 즉시 만료
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  // 특정 쿠키 값 가져오기
  public Optional<String> getCookieValue(HttpServletRequest request, String name) {
    Cookie cookie = WebUtils.getCookie(request, name);
    return cookie != null ? Optional.of(cookie.getValue()) : Optional.empty();
  }
}
