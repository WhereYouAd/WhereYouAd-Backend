package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickAnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickAnomalyEventRepository extends JpaRepository<ClickAnomalyEvent, Long> {

    // 광고계정 연동 해제 정리용
    // ad_content_id 는 FK 가 아니라 순수 컬럼이므로 조인 경로로 대상을 좁힌다
    // AdContent 가 cascade 로 삭제되기 전에 호출되어야 한다
    @Modifying
    @Query(value = "DELETE FROM click_anomaly_event " +
            "WHERE ad_content_id IN (" +
            "  SELECT ac.ad_content_id FROM ad_content ac " +
            "  JOIN ad_group ag ON ac.ad_group_id = ag.ad_group_id " +
            "  JOIN ad_campaign camp ON ag.ad_campaign_id = camp.ad_campaign_id " +
            "  WHERE camp.platform_account_id = :platformAccountId" +
            ") " +
            "LIMIT :batchSize",
            nativeQuery = true)
    int deleteByPlatformAccountIdInBatch(@Param("platformAccountId") Long platformAccountId,
                                         @Param("batchSize") int batchSize);

    // 조직 Hard Delete 정리용 안전망
    // org_id 도 FK 가 아니라 순수 컬럼이므로 조직 삭제로는 자동 정리되지 않는다
    @Modifying
    @Query("DELETE FROM ClickAnomalyEvent e WHERE e.orgId = :orgId")
    void deleteByOrgId(@Param("orgId") Long orgId);
}
