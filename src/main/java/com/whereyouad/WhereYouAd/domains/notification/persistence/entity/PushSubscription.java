package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "push_subscription",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_subscription_endpoint", columnNames = "endpoint"),
        indexes = @Index(name = "idx_push_subscription_membership", columnList = "membership_id")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_subscription_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OrgMember orgMember;

    // 브라우저가 발급한 push service endpoint (Chrome=FCM, Firefox=Mozilla, Safari=Apple)
    @Column(name = "endpoint", nullable = false, length = 512)
    private String endpoint;

    // 클라이언트 공개키 (Base64URL 65바이트 → 최대 88자)
    @Column(name = "p256dh_key", nullable = false, length = 128)
    private String p256dhKey;

    // 인증 secret (Base64URL 16바이트 → 최대 24자)
    @Column(name = "auth_secret", nullable = false, length = 32)
    private String authSecret;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    // 브라우저가 알려주는 만료 시각 (선택, 대개 null)
    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;
}
