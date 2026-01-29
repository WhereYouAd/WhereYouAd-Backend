package com.whereyouad.WhereYouAd.global.security.oauth2.service;

import com.whereyouad.WhereYouAd.global.security.oauth2.dto.*;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.UserSignUpException;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.AuthAccountConverter;
import com.whereyouad.WhereYouAd.domains.user.application.mapper.UserConverter;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthProviderAccountRepository authProviderAccountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 소셜 로그인 사용자 정보 불러오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 서비스 제공자 이름 (naver, google, kakako)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 소셜 enum type으로 변환
        Provider provider = Provider.fromRegistrationId(registrationId);

        // 소셜 로그인을 담을 객체(네이버, 구글, 카카오)
        OAuth2Response oAuth2Response = null;

        if (provider == Provider.NAVER) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (provider == Provider.GOOGLE) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else if (provider == Provider.KAKAO){
            oAuth2Response = new KaKaoResponse(oAuth2User.getAttributes());
        } else {
            throw new UserSignUpException(UserErrorCode.NOT_PROVIDE_SOCIAL);
        }

        // 사용자 소셜 로그인 고유 id (제공자 + 소셜 발급 id) -> (중복 회원 가입 방지)
        String providerId = oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();

        AuthProviderAccount existAccount = authProviderAccountRepository.findByProviderId(providerId);

        User user;

        // 해당 소셜로 한번도 로그인 하지 않은 경우 -> DB에 저장
        if (existAccount == null) {

            Optional<User> userOptional = userRepository.findUserByEmail(oAuth2Response.getEmail());

            if (userOptional.isPresent()) {
                user = userOptional.get();
                // 기존 이메일에 해당하는 유저가 존재하지만 이메일 인증이 안된 경우 -> 연동 불가
                if (!user.isEmailVerified()) {
                    throw new UserSignUpException(UserErrorCode.USER_EMAIL_NOT_VERIFIED);
                }
            }
            // 신규 유저(기존 email X, 소셜 로그인 처음) -> DB에 저장
            else {
                OAuth2UserInfo authUserDTO = OAuth2UserInfo.builder()
                        .email(oAuth2Response.getEmail())
                        .name(oAuth2Response.getName())
                        .role("ROLE_USER")
                        .provider(oAuth2Response.getProvider())
                        .providerId(providerId)
                        .build();
                User newUser = UserConverter.toSocialUser(authUserDTO);
                user = userRepository.save(newUser);
            }

            // 소셜 로그인 정보 저장 (AuthProviderAccount 객체)
            AuthProviderAccount account = AuthAccountConverter.toAuthProviderAccount(provider, providerId, user);
            authProviderAccountRepository.save(account);
        }
        // 해당 소셜로 로그인 한 정보가 있다면 -> 유저 정보(소셜 이름) 갱신
        else {
            user = existAccount.getUser();
            user.updateProfile(oAuth2Response.getName());
        }

        // 공통: OAuth2UserInfo 생성 및 반환
        OAuth2UserInfo authUserDTO = UserConverter.toOAuth2UserInfo(user, oAuth2Response);
        return new CustomOAuth2User(authUserDTO);
    }
}
