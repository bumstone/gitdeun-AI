package com.hyetaekon.hyetaekon.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class  CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    log.error("Not Authenticated Request", authException);
    log.error("Request Uri : {}", request.getRequestURI());

    // UNAUTHORIZED ErrorCode 사용
    ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

    // ErrorResponse 생성
    ErrorResponse errorResponse = ErrorResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();

    // HTTP 응답 설정
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(errorCode.getHttpStatus().value());
    response.setCharacterEncoding("UTF-8");

    // JSON 응답 반환
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }

}

