package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.RoasProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {
    // 예산 조회용
    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND m.adContent.adGroup.adCampaign.status = 'ON_GOING'")
    BigDecimal sumAllSpendsByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND m.provider = :provider AND m.adContent.adGroup.adCampaign.status = 'ON_GOING'")
    BigDecimal sumSpendsByUserIdAndOrgIdAndProvider(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("provider") Provider provider);

  // 특정 조직(orgId)에 속한 모든 프로젝트의 지정된 기간(start~end) 동안
  // 플랫폼(provider)별 총 매출액과 총 광고비를 합산하여 RoasProjection 형태로 조회
  @Query("""
      SELECT
          mf.provider AS provider,
          SUM(mf.revenue) AS totalRevenue,
          SUM(mf.spend) AS totalSpend
      FROM MetricFact mf
      JOIN mf.project p
      WHERE p.organization.id = :orgId
        AND mf.timeBucket >= :start
        AND mf.timeBucket <= :end
      GROUP BY mf.provider
      """)
  List<RoasProjection> findRoasByOrgAndPeriod(
      @Param("orgId") Long orgId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
}
