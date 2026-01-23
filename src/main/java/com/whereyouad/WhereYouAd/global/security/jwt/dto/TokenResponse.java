package com.whereyouad.WhereYouAd.global.security.jwt.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder
public record TokenResponse(
        String grantType, //Bearer
        String accessToken,
        Long accessTokenExpireIn,
        @JsonIgnore //RefreshToken 은 본문으로 반환하지 않고 쿠키 값으로 반환
        String refreshToken
) {}
