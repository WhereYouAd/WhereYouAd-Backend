package com.whereyouad.WhereYouAd.domains.platform.application.dto.request;

public class PlatformRequest {

    public record PlatformAccount(
            String customerId,   // 네이버 고객 ID (X-Customer) → PlatformAccount.externalAccountId
            String apiKey,       // API 키 (AES 암호화됨) → PlatformConnection.authIdentifier
            String secretKey     // Secret (AES 암호화됨) → PlatformConnection.authCredential
    ) {}

    // 네이버 광고 API 수정에 사용
    public record UpdateNaverApiRequest(
            String customerId, // 만약 customerId 가 같다면 그대로 유지, 다르면 계정 정보 자체를 교체
            String apiKey,
            String secretKey
    ) {}
}
