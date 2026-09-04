package com.whereyouad.WhereYouAd.global.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    // @Timed 애노테이션을 실제로 동작시키는 AOP 빈
    // 스케줄러(@Scheduled) 메서드 등에 @Timed를 붙이면 실행시간/실패율이 자동으로 지표로 남는다.
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
