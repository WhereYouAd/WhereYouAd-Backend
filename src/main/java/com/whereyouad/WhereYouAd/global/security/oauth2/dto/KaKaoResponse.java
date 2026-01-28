package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

import java.util.Map;

public class KaKaoResponse implements OAuth2Response {

    private final Map<String, Object> attribute;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    // id, kakao_account {email, profile {nickname}} 형식의 계층 구조
    public KaKaoResponse(Map<String, Object> attribute) {
        this.attribute = attribute;
        this.kakaoAccount = (Map<String, Object>) attribute.get("kakao_account");
        this.profile = (kakaoAccount != null) ? (Map<String, Object>) kakaoAccount.get("profile") : null;
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attribute.get("id"));
    }

    @Override
    public String getEmail() {
        return String.valueOf(kakaoAccount.get("email"));
    }

    @Override
    public String getName() {
        return String.valueOf(profile.get("nickname"));
    }
}