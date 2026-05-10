package com.whereyouad.WhereYouAd.domains.platform.application.mapper;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Currency;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.PlatformStatus;
import com.whereyouad.WhereYouAd.domains.platform.domain.constant.Timezone;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;

public class PlatformConverter {

    // dto -> entity
    public static PlatformAccount toPlatformAccount(PlatformRequest.PlatformAccount dto, Organization organization) {
        return PlatformAccount.builder()
                .externalAccountId(dto.customerId())
                .provider(Provider.NAVER)
                .status(PlatformStatus.ACTIVE)
                .currency(Currency.KRW)
                .timezone(Timezone.ASIA)
                .organization(organization)
                .build();
    }

    // dto -> entity
    public static PlatformConnection toPlatformConnection(PlatformRequest.PlatformAccount dto, User user, PlatformAccount platformAccount) {
        return PlatformConnection.builder()
                .authType(AuthType.API_KEY)
                .authIdentifier(dto.apiKey())
                .authCredential(dto.secretKey())
                .user(user)
                .platformAccount(platformAccount)
                .build();
    }

    public static PlatformConnection toTempPlatformConnection(String customerId, String encryptedApiKey, String encryptedSecretKey) {
        PlatformAccount tempAccount = PlatformAccount.builder()
                .externalAccountId(customerId)
                .build();
        return PlatformConnection.builder()
                .authIdentifier(encryptedApiKey)
                .authCredential(encryptedSecretKey)
                .platformAccount(tempAccount)
                .build();
    }

    public static PlatformResponse.PlatformAccount toPlatformAccountResponse(PlatformAccount platformAccount) {
        return new PlatformResponse.PlatformAccount(
                platformAccount.getId(),
                platformAccount.getExternalAccountId(),
                platformAccount.getProvider(),
                platformAccount.getStatus()
        );
    }
}
