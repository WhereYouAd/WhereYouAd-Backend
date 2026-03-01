package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.RoasProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {

  // 특정 프로젝트(projectId)의 지정된 기간(start~end) 동안
  // 플랫폼(provider)별 총 매출액과 총 광고비를 합산하여 RoasProjection 형태로 조회
  @Query("""
      SELECT
          mf.provider AS provider,
          SUM(mf.revenue) AS totalRevenue,
          SUM(mf.spend) AS totalSpend
      FROM MetricFact mf
      WHERE mf.project.id = :projectId
        AND mf.timeBucket >= :start
        AND mf.timeBucket <= :end
      GROUP BY mf.provider
      """)
  List<RoasProjection> findRoasByProjectAndPeriod(
      @Param("projectId") Long projectId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
