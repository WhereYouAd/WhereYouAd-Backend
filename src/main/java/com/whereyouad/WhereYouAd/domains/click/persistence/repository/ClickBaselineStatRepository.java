package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickBaselineStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ClickBaselineStatRepository extends JpaRepository<ClickBaselineStat, Long> {

    // 스케줄러 실행당 1회의 배치 IN 조회 (풀스캔 없음, uk_baseline_ad_slot 인덱스 사용)
    List<ClickBaselineStat> findByAdContentIdInAndWeekdayAndHourOfDay(
            Collection<Long> adContentIds, int weekday, int hourOfDay);
}
