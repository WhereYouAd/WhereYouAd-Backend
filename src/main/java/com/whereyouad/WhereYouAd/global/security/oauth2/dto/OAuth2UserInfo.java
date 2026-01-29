package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuth2UserInfo {

    private String name;
    private String email;
    private String role;
    private Provider provider;
    private String providerId;
}
