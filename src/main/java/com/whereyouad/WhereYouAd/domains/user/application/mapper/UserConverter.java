package com.whereyouad.WhereYouAd.domains.user.application.mapper;

import com.whereyouad.WhereYouAd.domains.user.application.dto.response.EmailSentResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.PasswordResetResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.MyPageResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.OAuth2Response;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.OAuth2UserInfo;

import java.util.List;

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

    public static MyPageResponse toMyPageResponse(User user, String provider) {

        return new MyPageResponse(user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getPhoneNumber(),
                user.isEmailVerified(),
                provider
        );
    }

    public static EmailSentResponse toEmailSentResponseSuccess(String email) {
        return new EmailSentResponse("인증 코드를 이메일로 전송했습니다",
                email,
                180L,
                false,
                null);
    }

    public static EmailSentResponse toEmailSentResponseFail(String email, List<Provider> providers) {
        return new EmailSentResponse("이미 소셜 계정으로 가입된 이메일 입니다.",
                email,
                null,
                true,
                providers);
    }

    public static PasswordResetResponse toPasswordResetResponse(String email) {

        return new PasswordResetResponse("인증 코드를 이메일로 전송했습니다.",
                email,
                180L);
    }
}
