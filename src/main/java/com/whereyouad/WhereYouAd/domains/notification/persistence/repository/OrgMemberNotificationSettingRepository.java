package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrgMemberNotificationSettingRepository extends JpaRepository<OrgMemberNotificationSetting, Long> {

    // 맴버 알림 설정 일괄 조회
    List<OrgMemberNotificationSetting> findByMembershipIdIn(Collection<Long> membershipIds);
}
