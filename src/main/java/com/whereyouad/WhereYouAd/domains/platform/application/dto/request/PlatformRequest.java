package com.whereyouad.WhereYouAd.domains.platform.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PlatformRequest {

    public record PlatformAccount(
            @NotBlank String customerId,   // 네이버 고객 ID (X-Customer) → PlatformAccount.externalAccountId
            @NotBlank String apiKey,       // API 키 (AES 암호화됨) → PlatformConnection.authIdentifier
            @NotBlank String secretKey     // Secret (AES 암호화됨) → PlatformConnection.authCredential
    ) {}
}
