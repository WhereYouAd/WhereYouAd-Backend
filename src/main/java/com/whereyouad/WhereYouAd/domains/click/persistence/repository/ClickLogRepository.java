package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

    // 조직별 봇(의심) 클릭 일일 요약 집계 - idx_click_log_suspect_clicked 인덱스 range scan
    // 반환: [orgId, orgName, 총 의심 클릭수, 유니크 IP 수, 영향받은 광고 수]
    @Query("SELECT o.id, o.name, COUNT(cl), COUNT(DISTINCT cl.ipAddress), COUNT(DISTINCT ac.id) " +
            "FROM ClickLog cl " +
            "JOIN cl.adContent ac JOIN ac.adGroup ag JOIN ag.adCampaign c JOIN c.organization o " +
            "WHERE cl.isSuspect = true AND cl.clickedAt >= :start AND cl.clickedAt < :end " +
            "GROUP BY o.id, o.name")
    List<Object[]> summarizeSuspectClicksByOrg(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 조직 내 봇 클릭 상위 광고 - 반환: [광고명, 클릭수]
    @Query("SELECT ac.name, COUNT(cl) " +
            "FROM ClickLog cl " +
            "JOIN cl.adContent ac JOIN ac.adGroup ag JOIN ag.adCampaign c " +
            "WHERE c.organization.id = :orgId AND cl.isSuspect = true " +
            "AND cl.clickedAt >= :start AND cl.clickedAt < :end " +
            "GROUP BY ac.id, ac.name ORDER BY COUNT(cl) DESC")
    List<Object[]> findTopSuspectAdsByOrg(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
            Pageable pageable);

    // PlatformAccount 연동 해제 시 청크 단위 정리용
    // ad_content_id → ad_group_id → ad_campaign_id → platform_account_id 경유
    @Modifying
    @Query(value = "DELETE FROM click_log " +
            "WHERE ad_content_id IN (" +
            "  SELECT ac.ad_content_id FROM ad_content ac " +
            "  JOIN ad_group ag ON ac.ad_group_id = ag.ad_group_id " +
            "  JOIN ad_campaign camp ON ag.ad_campaign_id = camp.ad_campaign_id " +
            "  WHERE camp.platform_account_id = :platformAccountId" +
            ") " +
            "LIMIT :batchSize",
            nativeQuery = true)
    int deleteByPlatformAccountIdInBatch(
            @Param("platformAccountId") Long platformAccountId,
            @Param("batchSize") int batchSize
    );
}