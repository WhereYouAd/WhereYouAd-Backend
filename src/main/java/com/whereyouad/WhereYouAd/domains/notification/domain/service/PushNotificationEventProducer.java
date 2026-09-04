package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.PushNotificationEvent;

// 브라우저 푸시(웹 푸시) 이벤트를 Kafka 에 적재하기 위한 인터페이스
public interface PushNotificationEventProducer {

    void produce(PushNotificationEvent event);
}
