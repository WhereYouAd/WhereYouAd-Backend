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

    // 조직 대상 브라우저 푸시(웹 푸시) 발송.
    // 내부에서 Notification/UserNotification/NotificationDelivery(PENDING) 를 저장하고 Kafka 로 발행하며,
    // 실제 웹 푸시 전송은 Consumer 가 트랜잭션 밖에서 수행한다. linkUrl 은 Service Worker 가 클릭 시 사용.
    void sendBrowserPushToOrg(Long orgId, NotificationType type, String title, String body, String linkUrl);

    boolean isExternalAlarmActive(Long orgId, NotificationType type);

    // 설정한 채널이 실제로 동작하는지 테스트 발송
    void sendTest(Long orgId, NotificationRequest.TestSend request);

    NotificationResponse.NotificationHistoryList getHistory(Long userId, Long orgId, String encodedCursor, Integer size);

    void markAsRead(Long userId, Long orgId, Long userNotificationId);

    void markAllAsRead(Long userId, Long orgId);
}
