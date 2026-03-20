package com.whereyouad.WhereYouAd.infrastructure.client.click;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
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
    public void produce(ClickResponse.ClickEvent event) {
        ClickDto dto = ClickDto.builder()
                .adId(String.valueOf(event.adContentId()))
                .ipAddress(event.ipAddress())
                .device(event.userAgent())
                .clickedAt(event.clickedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .isDummy(false) // 실제 클릭
                .build();

        kafkaTemplate.send(TOPIC, dto.getAdId(), dto);
        log.debug("[Kafka] 실제 클릭 이벤트 발행: adId={}, ip={}", dto.getAdId(), dto.getIpAddress());
    }
}
