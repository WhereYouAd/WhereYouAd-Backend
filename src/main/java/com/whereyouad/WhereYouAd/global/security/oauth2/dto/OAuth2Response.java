package com.whereyouad.WhereYouAd.global.security.oauth2.dto;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

public interface OAuth2Response {

    //제공자 (Ex. naver, google, kakao)
    Provider getProvider();

    //제공자에서 발급해주는 아이디(번호)
    String getProviderId();

    //이메일
    String getEmail();

    //사용자 실명
    String getName();
}
