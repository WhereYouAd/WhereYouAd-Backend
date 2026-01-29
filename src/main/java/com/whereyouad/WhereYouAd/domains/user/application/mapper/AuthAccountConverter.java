package com.whereyouad.WhereYouAd.domains.user.application.mapper;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;

public class AuthAccountConverter {
    // dto -> entity
    public static AuthProviderAccount toAuthProviderAccount(Provider provider, String providerId, User user){
        return AuthProviderAccount.builder()
                .provider(provider)
                .providerId(providerId)
                .user(user)
                .build();
    }
}
