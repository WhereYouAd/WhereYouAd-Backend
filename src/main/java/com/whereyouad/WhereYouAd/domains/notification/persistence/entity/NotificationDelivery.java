package com.whereyouad.WhereYouAd.domains.notification.persistence.entity;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryChannel;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.DeliveryStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_delivery")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delivery_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private DeliveryChannel channel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private OrgMember orgMember;

    public void markSuccess() {
        this.status = DeliveryStatus.SUCCESS;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = DeliveryStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
        this.retryCount++;
    }
}
