package com.whereyouad.WhereYouAd.domains.dashboard.domain.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DashboardClickService {

    SseEmitter subscribe(Long userId ,Long orgId, String mode);

}
