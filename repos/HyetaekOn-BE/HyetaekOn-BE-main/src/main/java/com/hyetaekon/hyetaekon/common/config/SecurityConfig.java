package com.hyetaekon.hyetaekon.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.hyetaekon.hyetaekon.common.jwt.CustomAccessDeniedHandler;
import com.hyetaekon.hyetaekon.common.jwt.CustomAuthenticationEntryPoint;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetailsService;
import com.hyetaekon.hyetaekon.common.jwt.JwtAuthenticationFilter;
import com.hyetaekon.hyetaekon.common.jwt.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;
  private final CustomUserDetailsService customUserDetailsService;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
        .csrf((auth) -> auth.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin((auth) -> auth.disable())
        .httpBasic((auth) -> auth.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers((headerConfig) -> headerConfig
            .frameOptions(frameOptionsConfig -> frameOptionsConfig.disable()));

    // 경로별 인가 작업
    http
        .authorizeHttpRequests((auth) -> auth
            .requestMatchers("/api/posts/type", "/api/posts/type/**", "/api/posts/search").permitAll()
            .requestMatchers(SecurityPath.ADMIN_ENDPOINTS).hasRole("ADMIN")
            .requestMatchers(SecurityPath.USER_ENDPOINTS).hasAnyRole("USER", "ADMIN")
            .requestMatchers(SecurityPath.PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().permitAll()
        );

    // 예외 처리
    http
        .exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패 처리
            .accessDeniedHandler(customAccessDeniedHandler)); // 인가 실패 처리

    // JwtFilter 추가
    http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, objectMapper), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // CORS 설정을 위한 Bean 등록
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:3000"); // 개발 환경
    configuration.addAllowedOrigin("https://hyetaekon.netlify.app");
    configuration.addAllowedOrigin("https://hyetaek-on.co.kr"); // 혜택온 도메인
    configuration.addAllowedOrigin("https://www.hyetaek-on.co.kr");
    configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
    configuration.addAllowedHeader("*"); // 모든 헤더 허용
    configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(java.util.List.of("Authorization"));
    configuration.setAllowCredentials(true); // 인증 정보 허용 (쿠키 등)

    org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 적용
    return source;
  }

  // Authentication manager
  @Bean
  public AuthenticationManager authenticationManager(
      HttpSecurity http,
      PasswordEncoder passwordEncoder) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder
        = http.getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder
        .userDetailsService(customUserDetailsService)
        .passwordEncoder(passwordEncoder);

    return authenticationManagerBuilder.build();
  }

  // 비밀번호 암호화 저장을 위한 Encoder Bean 등록
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}