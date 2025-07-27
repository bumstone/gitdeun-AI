package com.hyetaekon.hyetaekon.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Value("${jwt.secret-key}")
    private String secretKey;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private static final String BEARER = "Bearer";
    private static final String ADMIN_API_PREFIX = "/api/admin";


    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        boolean isAdminRequest = requestURI.startsWith(ADMIN_API_PREFIX);
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // access token이 있고, BEARER로 시작한다면
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            String token = authHeader.substring(BEARER.length()).trim(); // trim 추가

            try {
                // 토큰 검증
                if (jwtTokenProvider.validateToken(token)) {
                    // 유효한 토큰: 유저 정보 가져옴
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);

                    // 관리자 API 접근 시 추가 검증
                    if (isAdminRequest && !hasAdminRole(authentication.getAuthorities())) {
                        log.warn("관리자 권한 없이 관리자 리소스에 접근 시도: {}", requestURI);
                        sendAccessDeniedResponse(response);
                        return;
                    }

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // 토큰이 유효하지 않은 경우 - 관리자 API는 차단, 일반 API는 진행
                    if (isAdminRequest) {
                        log.warn("유효하지 않은 토큰으로 관리자 리소스 접근 시도: {}", requestURI);
                        sendUnauthorizedResponse(response);
                        return;
                    }
                    // 일반 API는 인증 없이도 접근 가능한 경우가 있으므로 계속 진행
                }
            } catch (Exception e) {
                // 토큰 처리 중 예외 발생
                log.error("JWT 토큰 처리 중 오류 발생 - URI: {}, Error: {}", requestURI, e.getMessage());

                if (isAdminRequest) {
                    // 관리자 API는 예외 발생 시 차단
                    sendUnauthorizedResponse(response);
                    return;
                }
                // 일반 API는 인증 실패로 처리하고 서비스 계속 진행 (SecurityContext 비워둠)
                SecurityContextHolder.clearContext();
            }
        } else if (isAdminRequest) {
            // 관리자 API 접근 시 토큰 없으면 Unauthorized 응답
            log.warn("인증 없이 관리자 리소스에 접근 시도: {}", requestURI);
            sendUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasAdminRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    // 접근 거부 응답 처리(403 - 권한 없음)
    private void sendAccessDeniedResponse(HttpServletResponse response) throws IOException {
        log.warn("403 Forbidden - 접근 거부됨");

        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

    }

    // 인증 실패 응답 처리 (401 - 인증 실패)
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        log.warn("401 Unauthorized - 인증 실패");

        ErrorCode errorCode = ErrorCode.NO_TOKEN;
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .build();

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

    }
}
