package com.whereyouad.WhereYouAd.domains.platform.application.dto.request;

public class PlatformRequest {

    public record PlatformAccount(
            String customerId,   // 네이버 고객 ID (X-Customer) → PlatformAccount.externalAccountId
            String apiKey,       // API 키 (평문) → 암호화 후 PlatformConnection.authIdentifier
            String secretKey     // Secret (AES 암호화됨) → PlatformConnection.authCredential
    ) {}
}
