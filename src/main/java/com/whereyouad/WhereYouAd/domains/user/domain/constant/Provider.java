package com.whereyouad.WhereYouAd.domains.user.domain.constant;

import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("google", "구글"),
    NAVER("naver", "네이버"),
    KAKAO("kakao", "카카오");

    private final String registrationId;
    private final String description;

    // 문자열(registrationId)을 받아서 해당 이넘 반환
    public static Provider fromRegistrationId(String registrationId) {
        return Arrays.stream(Provider.values())
                .filter(socialType -> socialType.getRegistrationId().equals(registrationId))
                .findFirst()
                .orElseThrow(() -> new UserHandler(UserErrorCode.NOT_PROVIDE_SOCIAL));
    }
}
