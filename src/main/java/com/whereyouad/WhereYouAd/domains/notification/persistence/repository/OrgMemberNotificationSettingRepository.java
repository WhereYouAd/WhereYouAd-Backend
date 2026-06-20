package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgMemberNotificationSettingRepository extends JpaRepository<OrgMemberNotificationSetting, Long> {
}
