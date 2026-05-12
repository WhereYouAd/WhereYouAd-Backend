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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public static PlatformResponse.PlatformKeyResponse toPlatformKeyResponse(PlatformConnection connection) {
        return new PlatformResponse.PlatformKeyResponse(
                connection.getPlatformAccount().getId(),
                connection.getPlatformAccount().getExternalAccountId(),
                connection.getPlatformAccount().getProvider(),
                connection.getAuthType(),
                connection.getPlatformAccount().getStatus(),
                connection.getTokenExpireAt() != null ? LocalDate.from(connection.getTokenExpireAt()) : null,
                connection.getUpdatedAt() != null ? LocalDate.from(connection.getUpdatedAt()) : null
                // TODO : 마지막 연동일자? 를 나타내는 것에 대해 논의 필요
                // -> PlatformConnection 에 lastSyncedAt 로 필드를 추가하는게 나을지? 지금처럼 updatedAt 를 가져다 쓰는게 나을지?
        );
    }

    public static PlatformResponse.PlatformAccountListResponse toPlatformAccountListResponse(List<PlatformConnection> connections) {
        List<PlatformResponse.PlatformKeyResponse> responseList = new ArrayList<>();
        for (PlatformConnection connection : connections) {
            PlatformResponse.PlatformKeyResponse response = toPlatformKeyResponse(connection);
            responseList.add(response);
        }

        return new PlatformResponse.PlatformAccountListResponse(responseList);
    }
}
