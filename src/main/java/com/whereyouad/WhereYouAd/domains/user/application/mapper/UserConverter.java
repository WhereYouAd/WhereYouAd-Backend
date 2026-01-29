package com.whereyouad.WhereYouAd.domains.user.application.mapper;

import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.OAuth2Response;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.OAuth2UserInfo;

public class UserConverter {

    public static SignUpResponse toSignInResponse(User user) {
        return new SignUpResponse(user.getId(), user.getCreatedAt());
    }

    // dto -> entity
    public static User toSocialUser(OAuth2UserInfo authUserResponseDTO) {
        return User.builder()
                .email(authUserResponseDTO.getEmail())
                .name(authUserResponseDTO.getName())
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();
    }

    // entity -> dto
    public static OAuth2UserInfo toOAuth2UserInfo(User user, OAuth2Response oAuth2Response) {
        return OAuth2UserInfo.builder()
                .email(user.getEmail())
                .name(user.getName())
                .role("ROLE_USER")
                .providerId(oAuth2Response.getProviderId())
                .provider(oAuth2Response.getProvider())
                .build();
    }
}
