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

    @Column(name = "access_token_enc", length = 4000)
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc", length = 4000)
    private String refreshTokenEnc;

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
}
