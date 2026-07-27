package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

import java.time.Instant;

public record SocialOAuthCredential(
        Long providerAccountId,
        Provider provider,
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt
) {
    // API 호출 도중 만료되는 상황을 피하기 위해 실제 만료 시각보다 30초 먼저 갱신 대상으로 판단한다.
    public boolean hasUsableAccessToken() {
        return accessToken != null
                && (accessTokenExpiresAt == null || Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(30)));
    }
}
