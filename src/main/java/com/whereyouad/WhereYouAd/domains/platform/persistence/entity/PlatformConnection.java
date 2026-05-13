package com.whereyouad.WhereYouAd.domains.platform.persistence.entity;

import com.whereyouad.WhereYouAd.domains.platform.domain.constant.AuthType;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_connection")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class PlatformConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    @ColumnDefault("'API_KEY'")
    private AuthType authType;

    @Column(name = "auth_identifier", length = 4000)
    private String authIdentifier; // 주 인증 정보 (예: OAuth Access Token, API Client ID, Login ID)

    @Column(name = "auth_credential", length = 4000)
    private String authCredential; // 부 인증 정보 (예: OAuth Refresh Token, API Client Secret, Password)

    @Column(name = "token_expire_at")
    private LocalDateTime tokenExpireAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_account_id")
    private PlatformAccount platformAccount;

    public void renewOAuth(String authIdentifier, LocalDateTime tokenExpireAt) {
        this.authType = AuthType.OAUTH;
        this.authIdentifier = authIdentifier;
        this.tokenExpireAt = tokenExpireAt;
        this.revokedAt = null;
    }

    public void updateAuth(String authIdentifier, String authCredential, LocalDateTime tokenExpireAt) {
        this.authIdentifier = authIdentifier;
        this.authCredential = authCredential;
        this.tokenExpireAt = tokenExpireAt;
    }

    public void renewApiKey(String authIdentifier, String authCredential) {
        this.authType = AuthType.API_KEY;
        this.authIdentifier = authIdentifier;
        this.authCredential = authCredential;
        this.tokenExpireAt = null;
        this.revokedAt = null;
    }
}
