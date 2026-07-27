package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SocialOAuthUnlinkService {

    private final SocialOAuthTokenService socialOAuthTokenService;
    private final Map<Provider, SocialOAuthUnlinkClient> clients = new EnumMap<>(Provider.class);

    public SocialOAuthUnlinkService(
            SocialOAuthTokenService socialOAuthTokenService,
            List<SocialOAuthUnlinkClient> unlinkClients
    ) {
        this.socialOAuthTokenService = socialOAuthTokenService;
        unlinkClients.forEach(client -> clients.put(client.provider(), client));
    }

    public void unlinkAll(Long userId) {
        List<SocialOAuthCredential> credentials = socialOAuthTokenService.getCredentialsForWithdrawal(userId);
        for (SocialOAuthCredential credential : credentials) {
            SocialOAuthUnlinkClient client = clients.get(credential.provider());
            if (client == null) {
                throw new UserHandler(UserErrorCode.NOT_PROVIDE_SOCIAL);
            }
            socialOAuthTokenService.markUnlinking(credential.providerAccountId());
            try {
                client.unlink(credential);
                socialOAuthTokenService.markUnlinked(credential.providerAccountId());
            } catch (RuntimeException e) {
                recordFailure(credential.providerAccountId(), e);
                throw e;
            }
        }
    }

    private void recordFailure(Long providerAccountId, RuntimeException cause) {
        String failureCode = cause instanceof AppException appException
                ? appException.getErrorCode().getCode()
                : cause.getClass().getSimpleName();
        try {
            socialOAuthTokenService.markUnlinkFailed(providerAccountId, failureCode);
        } catch (RuntimeException stateException) {
            log.error("소셜 연동 해제 실패 상태 저장 실패: providerAccountId={}", providerAccountId, stateException);
            cause.addSuppressed(stateException);
        }
    }
}
