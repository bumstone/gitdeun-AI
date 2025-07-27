package com.hyetaekon.hyetaekon.common.jwt;

public interface CustomUserPrincipal {
    String getRealId();
    String getNickname();
    String getRole();
    String getName();
}
