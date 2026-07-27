package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2UserInfo authUserDTO;
    private final Provider provider;

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return authUserDTO.getRole();
            }
        });
        return collection;
    }

    @Override
    public String getName() {
        return authUserDTO.getProviderId();
    }

    public String getProviderId() {
        return authUserDTO.getProviderId();
    }

    public String getEmail() {
        return authUserDTO.getEmail();
    }

    public Provider getProvider() {
        return provider;
    }
}
