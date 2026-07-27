package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        // Provider를 기준으로 각 소셜 연동 해제 구현체를 바로 찾을 수 있도록 등록한다.
        unlinkClients.forEach(client -> clients.put(client.provider(), client));
    }

    // 회원과 연결된 모든 소셜 계정의 연동을 순서대로 해제한다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void unlinkAll(Long userId) {
        // 이미 해제된 계정은 제외하므로 이메일 회원은 빈 목록으로 종료된다.
        List<SocialOAuthCredential> credentials = socialOAuthTokenService.getCredentialsForWithdrawal(userId);
        for (SocialOAuthCredential credential : credentials) {
            // 계정의 Provider와 일치하는 연동 해제 클라이언트를 선택한다.
            SocialOAuthUnlinkClient client = clients.get(credential.provider());
            if (client == null) {
                throw new UserHandler(UserErrorCode.NOT_PROVIDE_SOCIAL);
            }
            // 외부 API 호출 전에 시도 상태와 횟수를 먼저 기록한다.
            socialOAuthTokenService.markUnlinking(credential.providerAccountId());
            try {
                client.unlink(credential);
                // 연동 해제가 완료되면 더 이상 필요하지 않은 OAuth 토큰을 제거한다.
                socialOAuthTokenService.markUnlinked(credential.providerAccountId());
            } catch (RuntimeException e) {
                // 실패 상태를 남기고 예외를 다시 전달하여 내부 회원탈퇴도 진행되지 않게 한다.
                recordFailure(credential.providerAccountId(), e);
                throw e;
            }
        }
    }

    private void recordFailure(Long providerAccountId, RuntimeException cause) {
        // 비즈니스 예외는 정의된 오류 코드를, 그 외 예외는 타입명을 저장한다.
        String failureCode = cause instanceof AppException appException
                ? appException.getErrorCode().getCode()
                : cause.getClass().getSimpleName();
        try {
            socialOAuthTokenService.markUnlinkFailed(providerAccountId, failureCode);
        } catch (RuntimeException stateException) {
            log.error("소셜 연동 해제 실패 상태 저장 실패: providerAccountId={}", providerAccountId, stateException);
            // 상태 저장 예외가 실제 연동 해제 실패 원인을 덮지 않도록 보조 예외로 추가한다.
            cause.addSuppressed(stateException);
        }
    }
}
