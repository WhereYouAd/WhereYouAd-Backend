package com.whereyouad.WhereYouAd.domains.notification.domain.service;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.domain.constant.NotificationType;

public interface NotificationService {

    NotificationResponse.MySettings getMySettings(Long userId, Long orgId);

    void updateMaster(Long userId, Long orgId, NotificationRequest.UpdateMaster request);

    void updateChannels(Long userId, Long orgId, NotificationRequest.UpdateChannels request);

    void updateAlerts(Long userId, Long orgId, NotificationRequest.UpdateAlerts request);

    void updateOrgSettings(Long userId, Long orgId, NotificationRequest.UpdateOrgSettings request);

    NotificationResponse.MemberSettingList getMemberSettings(Long userId, Long orgId, String encodedCursor, Integer size);

    void updateMemberSettings(Long userId, Long orgId, NotificationRequest.BulkUpdateMembers request);

    void sendApiAlarmToOrg(Long orgId, NotificationType type, String title, String message);

    // 설정한 채널이 실제로 동작하는지 테스트 발송
    void sendTest(Long orgId, NotificationRequest.TestSend request);
}
