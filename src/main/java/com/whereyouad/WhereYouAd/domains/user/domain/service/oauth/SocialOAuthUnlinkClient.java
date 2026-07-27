package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

public interface SocialOAuthUnlinkClient {
    Provider provider();

    void unlink(SocialOAuthCredential credential);
}
