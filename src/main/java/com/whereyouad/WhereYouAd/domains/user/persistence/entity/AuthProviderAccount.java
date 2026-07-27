package com.whereyouad.WhereYouAd.domains.user.persistence.entity;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.OAuthUnlinkStatus;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_provider_account")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AuthProviderAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "provider_account_id")
    private Long id;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "oauth_access_token", length = 4096)
    private String oauthAccessToken;

    @Column(name = "oauth_refresh_token", length = 4096)
    private String oauthRefreshToken;

    @Column(name = "oauth_access_token_expires_at")
    private LocalDateTime oauthAccessTokenExpiresAt;

    @Column(name = "unlinked_at")
    private LocalDateTime unlinkedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "unlink_status", length = 20)
    private OAuthUnlinkStatus unlinkStatus = OAuthUnlinkStatus.LINKED;

    @Column(name = "unlink_attempted_at")
    private LocalDateTime unlinkAttemptedAt;

    @Column(name = "unlink_failure_code", length = 100)
    private String unlinkFailureCode;

    @Builder.Default
    @Column(name = "unlink_retry_count", nullable = false)
    private int unlinkRetryCount = 0;

    @CreatedDate
    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    //연관 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void updateOAuthTokens(
            String encryptedAccessToken,
            String encryptedRefreshToken,
            LocalDateTime accessTokenExpiresAt
    ) {
        this.oauthAccessToken = encryptedAccessToken;
        if (encryptedRefreshToken != null) {
            this.oauthRefreshToken = encryptedRefreshToken;
        }
        this.oauthAccessTokenExpiresAt = accessTokenExpiresAt;
        this.unlinkedAt = null;
        this.unlinkStatus = OAuthUnlinkStatus.LINKED;
        this.unlinkAttemptedAt = null;
        this.unlinkFailureCode = null;
        this.unlinkRetryCount = 0;
    }

    public void markUnlinking() {
        this.unlinkStatus = OAuthUnlinkStatus.UNLINKING;
        this.unlinkAttemptedAt = LocalDateTime.now();
        this.unlinkFailureCode = null;
        this.unlinkRetryCount++;
    }

    public void markUnlinkFailed(String failureCode) {
        this.unlinkStatus = OAuthUnlinkStatus.UNLINK_FAILED;
        this.unlinkFailureCode = failureCode;
    }

    public void clearOAuthTokens() {
        this.oauthAccessToken = null;
        this.oauthRefreshToken = null;
        this.oauthAccessTokenExpiresAt = null;
        this.unlinkedAt = LocalDateTime.now();
        this.unlinkStatus = OAuthUnlinkStatus.UNLINKED;
        this.unlinkFailureCode = null;
    }

}
