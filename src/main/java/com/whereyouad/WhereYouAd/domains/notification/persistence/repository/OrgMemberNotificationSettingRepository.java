package com.whereyouad.WhereYouAd.domains.notification.persistence.repository;

import com.whereyouad.WhereYouAd.domains.notification.persistence.entity.OrgMemberNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrgMemberNotificationSettingRepository extends JpaRepository<OrgMemberNotificationSetting, Long> {

    // 맴버 알림 설정 일괄 조회
    List<OrgMemberNotificationSetting> findByMembershipIdIn(Collection<Long> membershipIds);

    // 조직 내 브라우저 푸시 수신 가능한 활성 멤버 조회
    // 브라우저 푸시 채널 ON + 마스터 ON + 요청 알림 종류 ON (클릭/리포트) + 유저 ACTIVE
    @Query("SELECT s FROM OrgMemberNotificationSetting s " +
            "JOIN FETCH s.orgMember om " +
            "JOIN FETCH om.user u " +
            "WHERE om.organization.id = :orgId " +
            "AND s.isMasterEnabled = true " +
            "AND s.isBrowserPushEnabled = true " +
            "AND u.status = 'ACTIVE' " +
            "AND ((:alertClicks = true AND s.alertClicks = true) " +
            "  OR (:alertReport = true AND s.alertReport = true))")
    List<OrgMemberNotificationSetting> findPushEnabledForOrg(
            @Param("orgId") Long orgId,
            @Param("alertClicks") boolean alertClicks,
            @Param("alertReport") boolean alertReport);
}
