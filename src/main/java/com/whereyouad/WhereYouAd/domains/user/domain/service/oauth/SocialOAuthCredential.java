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
    public boolean hasUsableAccessToken() {
        return accessToken != null
                && (accessTokenExpiresAt == null || Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(30)));
    }
}
