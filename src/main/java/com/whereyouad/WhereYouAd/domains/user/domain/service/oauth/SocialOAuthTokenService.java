package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.exception.code.UserErrorCode;
import com.whereyouad.WhereYouAd.domains.user.exception.handler.UserHandler;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.AuthProviderAccount;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.AuthProviderAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialOAuthTokenService {

    private static final ZoneId TOKEN_TIME_ZONE = ZoneId.of("Asia/Seoul");

    private final AuthProviderAccountRepository authProviderAccountRepository;
    private final OAuthTokenCryptoService tokenCryptoService;

    @Transactional
    public void saveTokens(
            String email,
            Provider provider,
            OAuth2AccessToken accessToken,
            OAuth2RefreshToken refreshToken
    ) {
        AuthProviderAccount account = authProviderAccountRepository.findByUser_EmailAndProvider(email, provider)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));

        try {
            String encryptedAccessToken = encrypt(accessToken.getTokenValue());
            String encryptedRefreshToken = refreshToken == null ? null : encrypt(refreshToken.getTokenValue());
            LocalDateTime expiresAt = toLocalDateTime(accessToken.getExpiresAt());

            account.updateOAuthTokens(encryptedAccessToken, encryptedRefreshToken, expiresAt);
        } catch (GeneralSecurityException e) {
            throw new UserHandler(UserErrorCode.SOCIAL_TOKEN_PROCESSING_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<SocialOAuthCredential> getCredentialsForWithdrawal(Long userId) {
        return authProviderAccountRepository.findByUser_IdAndUnlinkedAtIsNull(userId).stream()
                .map(this::toCredential)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUnlinking(Long providerAccountId) {
        AuthProviderAccount account = findAccount(providerAccountId);
        account.markUnlinking();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUnlinked(Long providerAccountId) {
        AuthProviderAccount account = findAccount(providerAccountId);
        account.clearOAuthTokens();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUnlinkFailed(Long providerAccountId, String failureCode) {
        AuthProviderAccount account = findAccount(providerAccountId);
        account.markUnlinkFailed(failureCode);
    }

    private AuthProviderAccount findAccount(Long providerAccountId) {
        return authProviderAccountRepository.findById(providerAccountId)
                .orElseThrow(() -> new UserHandler(UserErrorCode.USER_NOT_FOUND));
    }

    private SocialOAuthCredential toCredential(AuthProviderAccount account) {
        try {
            String accessToken = decryptNullable(account.getOauthAccessToken());
            String refreshToken = decryptNullable(account.getOauthRefreshToken());
            if (accessToken == null && refreshToken == null) {
                throw new UserHandler(UserErrorCode.SOCIAL_REAUTH_REQUIRED);
            }

            return new SocialOAuthCredential(
                    account.getId(),
                    account.getProvider(),
                    accessToken,
                    refreshToken,
                    toInstant(account.getOauthAccessTokenExpiresAt())
            );
        } catch (GeneralSecurityException e) {
            throw new UserHandler(UserErrorCode.SOCIAL_TOKEN_PROCESSING_FAILED);
        }
    }

    private String encrypt(String token) throws GeneralSecurityException {
        return tokenCryptoService.encrypt(token);
    }

    private String decryptNullable(String encryptedToken) throws GeneralSecurityException {
        if (encryptedToken == null || encryptedToken.isBlank()) {
            return null;
        }
        return tokenCryptoService.decrypt(encryptedToken);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, TOKEN_TIME_ZONE);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(TOKEN_TIME_ZONE).toInstant();
    }
}
