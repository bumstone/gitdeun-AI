package com.hyetaekon.hyetaekon.common.jwt;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refreshToken")
public class RefreshToken {

    @Id
    private String refreshToken;

    private String realId;
    private Long issuedAt;

    // Time to live (TTL) 설정, Redis에 만료 시간을 설정
    @TimeToLive
    private Long ttl;
}