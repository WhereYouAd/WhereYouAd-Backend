package com.whereyouad.WhereYouAd.infrastructure.client.kafka;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.domain.service.ClickEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaClickEventProducer implements ClickEventProducer {

    private static final String TOPIC = "ad-click-events";

    private final KafkaTemplate<String, ClickDto> kafkaTemplate;

    @Override
    public void produce(ClickDto event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getAdContentId()), event);
        log.debug("[Kafka] 실제 클릭 이벤트 발행: adId={}, ip={}", event.getAdContentId(), event.getIpAddress());
    }
}
