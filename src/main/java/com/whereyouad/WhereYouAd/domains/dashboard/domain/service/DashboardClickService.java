package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DashboardClickService {

    SseEmitter subscribe(Long userId ,Long orgId, Provider provider);

}
