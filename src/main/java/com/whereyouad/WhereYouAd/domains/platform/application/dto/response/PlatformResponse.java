package com.whereyouad.WhereYouAd.domains.platform.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;

public class PlatformResponse {

    public record PlatformAccount(
            Long platformAccountId,
            String externalAccountId,
            Provider provider,
            PlatformStatus status
    ) {}
}
