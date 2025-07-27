package com.hyetaekon.hyetaekon.common.util;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticateUser {
  // 인증된 사용자면 userId, 그렇지않으면 0 반환
  public Long authenticateUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
      return 0L; // 인증되지 않은 사용자일 경우 기본 값
    }

    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    return userDetails.getId();
  }
}
