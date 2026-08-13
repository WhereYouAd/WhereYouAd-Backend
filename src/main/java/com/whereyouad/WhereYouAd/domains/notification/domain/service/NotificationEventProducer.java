package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.NotificationAlertEvent;

// 외부 채널(디스코드/슬랙) 알림 이벤트를 Kafka에 적재하기 위한 인터페이스
public interface NotificationEventProducer {

    void produce(NotificationAlertEvent event);
}
