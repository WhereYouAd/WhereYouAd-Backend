package com.whereyouad.WhereYouAd.domains.platform.application.dto.response;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;

import java.time.LocalDate;
import java.util.List;

public class PlatformResponse {

    public record PlatformAccount(
            Long platformAccountId,
            String externalAccountId,
            Provider provider,
            PlatformStatus status
    ) {}

    public record PlatformKeyResponse(
            Long platformAccountId,
            String externalAccountId,
            Provider provider,
            AuthType authType,
            PlatformStatus status,
            LocalDate tokenExpireAt,
            LocalDate syncedAt
    ) {}

    public record PlatformAccountListResponse(
            List<PlatformKeyResponse> platformAccounts
    ) {}
}
