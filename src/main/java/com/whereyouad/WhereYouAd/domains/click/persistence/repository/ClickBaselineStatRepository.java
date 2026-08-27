package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ClickBaselineStatRepository extends JpaRepository<ClickBaselineStat, Long> {

    // 스케줄러 실행당 1회의 배치 IN 조회 (풀스캔 없음, uk_baseline_ad_slot 인덱스 사용)
    List<ClickBaselineStat> findByAdContentIdInAndWeekdayAndHourOfDay(
            Collection<Long> adContentIds, int weekday, int hourOfDay);

    // 광고계정 연동 해제 정리용 (조직 정보가 없어 JOIN 사용)
    @Modifying
    @Query(value = "DELETE FROM click_baseline_stat " +
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
}
