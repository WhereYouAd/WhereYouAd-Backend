package com.whereyouad.WhereYouAd.domains.notification.application.dto;

import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 디스코드/슬랙 등 외부 채널 알림 발송을 위해 Kafka로 발행되는 이벤트
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationAlertEvent {
    private Long orgId;
    private NotificationType type;
    private String title;
    private String message;
}
