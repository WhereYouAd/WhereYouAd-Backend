package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;

public interface OrgNotificationSettingService {

    // 조직의 슬랙/디스코드 설정 저장(없으면 생성). 조직 ADMIN 만 가능
    NotificationResponse.ChannelsListResponse upsertChannels(Long userId, Long orgId, NotificationRequest.ChannelSettingRequest request);

    // 조직의 현재 채널 설정 조회
    NotificationResponse.ChannelsListResponse getChannels(Long userId, Long orgId);

    // 설정한 채널이 실제로 동작하는지 테스트 발송
    void sendTest(Long orgId, NotificationRequest.TestSend request);
}
