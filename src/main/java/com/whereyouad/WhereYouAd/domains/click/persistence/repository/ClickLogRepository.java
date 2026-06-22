package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickLogRepository extends JpaRepository<ClickLog, Long> {

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