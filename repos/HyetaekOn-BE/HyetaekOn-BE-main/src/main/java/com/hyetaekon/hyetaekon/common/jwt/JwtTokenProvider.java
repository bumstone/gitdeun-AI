package com.hyetaekon.hyetaekon.common.jwt;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.user.entity.Role;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Getter
@Component
public class JwtTokenProvider {

    private final Key secretKey;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private BlacklistService blackListService;

    @Autowired
    private JwtTokenParser jwtTokenParser;

    @Value("${jwt.access-expired}")
    private Long accessTokenExpired;

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;


    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성 - 유저 정보 이용
    public JwtToken generateToken(Authentication authentication) {

        long now = (new Date()).getTime();
        Date accessTokenExpiration = new Date(now + accessTokenExpired * 1000);

        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

        String jti = UUID.randomUUID().toString();
        // Access Token 생성
        String accessToken = Jwts.builder()
            .setSubject(userPrincipal.getRealId()) // 이메일을 Subject로 설정
            .setIssuedAt(new Date()) // 발행 시간
            .setId(jti)  // blacklist 관리를 위한 jwt token id
            .claim("nickname", userPrincipal.getNickname()) // 닉네임
            .claim("role", userPrincipal.getRole()) // 사용자 역할(Role)
            .claim("name",userPrincipal.getName())
            .setExpiration(accessTokenExpiration) // 만료 시간
            .signWith(secretKey, SignatureAlgorithm.HS256) // 서명
            .compact();

        // Refresh Token 생성 (임의의 값 생성)
        String refreshToken = UUID.randomUUID().toString();

        // Redis에 Refresh Token 정보 저장
        refreshTokenService.saveRefreshToken( refreshToken, userPrincipal.getRealId(), refreshTokenExpired);


        // JWT Token 객체 반환
        return JwtToken.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

    }


    // 토큰에서 유저 정보 추출
    public Authentication getAuthentication(String accessToken) {
        // 토큰에서 Claims 추출
        Claims claims = jwtTokenParser.parseClaims(accessToken);

        // 권한 정보 확인
        if (claims.get("role") == null) {
            throw new GlobalException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 사용자 정보 추출
        String realId = claims.getSubject(); // 토큰 subject에서 realId 추출
        String nickname = claims.get("nickname").toString(); // nickname 추출
        String password = claims.get("password", String.class);
        String name = claims.get("name", String.class);

        String roleName = claims.get("role", String.class); // 문자열로 읽기

        // 문자열에서 Role 객체로 변환
        Role role = Role.valueOf(roleName); // Enum이라면 가능

        // realId로 User 조회
        User user = userRepository.findByRealIdAndDeletedAtIsNull(realId)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자 ID 가져오기
        Long id = user.getId();

        // CustomUserDetails 생성
        CustomUserDetails userDetails = new CustomUserDetails(id, realId, nickname, role, password, name);

        // 문자열을 GrantedAuthority로 변환
        Collection<? extends GrantedAuthority> authorities =
            Collections.singletonList(() -> roleName);


        // Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    // 토큰 정보 검증
    public boolean validateToken(String token) {
        log.debug("validateToken start");
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

            String jti = claims.getId(); // JTI 추출
            return !blackListService.isTokenBlacklisted(jti);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty.", e);
        }
        return false;
    }

}
