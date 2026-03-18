package com.whereyouad.WhereYouAd.infrastructure.client.click;

import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;

// 클릭 로그를 큐(지금은 redis, 추후에 kafka)에 적재하기 위한 인터페이스
public interface ClickEventPublisher {
    
    void publish(ClickResponse.ClickEvent event);
}