package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class GoogleResponse implements OAuth2Response{

    private final Map<String, Object> attribute;

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attribute.get("sub"));
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
