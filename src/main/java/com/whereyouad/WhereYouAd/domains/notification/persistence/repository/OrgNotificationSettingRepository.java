package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgNotificationSettingRepository extends JpaRepository<OrgNotificationSetting, Long> {
}
