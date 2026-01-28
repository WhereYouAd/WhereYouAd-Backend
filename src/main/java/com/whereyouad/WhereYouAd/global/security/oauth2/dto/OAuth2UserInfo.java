package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuth2UserInfo {

    public String name;
    public String email;
    public String role;
    public Provider provider;
    public String providerId;
}
