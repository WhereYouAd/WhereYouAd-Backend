package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

public interface SocialOAuthUnlinkClient {
    // 구현체가 담당하는 소셜 제공자를 반환한다.
    Provider provider();

    // 저장된 OAuth 인증 정보로 소셜 제공자의 연동을 해제한다.
    void unlink(SocialOAuthCredential credential);
}
