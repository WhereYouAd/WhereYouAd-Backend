package com.whereyouad.WhereYouAd.domains.notification.domain.service;

public interface NotificationService {

    void sendApiAlarmToOrg(Long orgId, String title, String message);
}
