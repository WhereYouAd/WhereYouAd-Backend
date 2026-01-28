package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

import java.util.Map;

public class NaverResponse implements OAuth2Response{

    private final Map<String, Object> attribute;

    // 네이버는 response라는 안에 실제 데이터 있어서 추출해서 저장
    public NaverResponse(Map<String, Object> attribute) {
        this.attribute = (Map<String, Object>) attribute.get("response");
    }

    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attribute.get("id"));
    }

    @Override
    public String getEmail() {
        return String.valueOf(attribute.get("email"));
    }

    @Override
    public String getName() {
        return String.valueOf(attribute.get("name"));
    }
}
