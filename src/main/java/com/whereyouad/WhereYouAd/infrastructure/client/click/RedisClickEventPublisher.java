package com.whereyouad.WhereYouAd.infrastructure.client.click;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.whereyouad.WhereYouAd.global.utils.RedisUtil;
import org.springframework.stereotype.Component;

@Slf4j
@Component // TODO: kafka 도입 시 어노테이션 제거
@RequiredArgsConstructor
// 클릭 이벤트를 redis에 LPush하는 메서드
public class RedisClickEventPublisher implements ClickEventPublisher {

    private static final String QUEUE_NAME = "click_queue";

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(ClickResponse.ClickEvent event) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(event);
            redisUtil.leftPush(QUEUE_NAME, jsonEvent);
            log.debug("redis 큐에 클릭 이벤트 저장: {}", jsonEvent);
        }
        catch (JsonProcessingException e) {
            log.error("Json 객체 직렬화 실패: {}", event, e);
        }
        catch (Exception e) {
            log.error("redis 큐에 클릭 이벤트 저장 실패: {}", event, e);
        }
    }
}
