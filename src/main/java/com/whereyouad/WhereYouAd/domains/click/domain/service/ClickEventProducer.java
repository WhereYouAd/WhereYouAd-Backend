package com.whereyouad.WhereYouAd.domains.click.domain.service;

import com.whereyouad.WhereYouAd.domains.click.application.dto.ClickDto;

// 클릭 이벤트를 Kafka에 적재하기 위한 인터페이스
public interface ClickEventProducer {

    void produce(ClickDto event);
}
