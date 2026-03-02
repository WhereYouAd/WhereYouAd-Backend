package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {
    // 예산 조회용
    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND m.adContent.adGroup.adCampaign.status = 'ON_GOING'")
    BigDecimal sumAllSpendsByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND m.provider = :provider AND m.adContent.adGroup.adCampaign.status = 'ON_GOING'")
    BigDecimal sumSpendsByUserIdAndOrgIdAndProvider(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("provider") Provider provider);

    //전체 지표 조회 로직에서 사용
    // 해당 프로젝트의 가장 최신 데이터 날짜를 가져오는 쿼리
    @Query("SELECT MAX(m.timeBucket) FROM MetricFact m")
    Optional<LocalDateTime> findLatestTimeBucket();

    // orgId에 속한 모든 프로젝트의 지표중 해당 MetricFact 가 속한 AdCampaign 의 status 가 ON_GOING 인 지표를 지정된 기간 범위 합산
    @Query("SELECT " +
            "COALESCE(SUM(m.impressions), 0) AS totalImpressions, " +
            "COALESCE(SUM(m.clicks), 0) AS totalClicks, " +
            "COALESCE(SUM(m.conversions), 0) AS totalConversions, " +
            "COALESCE(SUM(m.spend), 0) AS totalSpend, " +
            "COALESCE(SUM(m.revenue), 0) AS totalRevenue " +
            "FROM MetricFact m " +
            "JOIN m.project p " +
            "JOIN m.adContent ac " +
            "JOIN ac.adGroup ag " +
            "JOIN ag.adCampaign camp " +
            "WHERE p.organization.id = :orgId " +
            "AND camp.status = :status " +
            "AND m.timeBucket >= :startDate AND m.timeBucket < :endDate")
    MetricSumProjection findMetricsSumByOrgIdAndDateRange(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Status status
            );

    //orgId 와 provider 가 일치하고 해당 MetricFact 가 속한 AdCampaign 의 status 가 ON_GOING 인 지표에 대해 지정된 기간 범위 합산
    @Query("SELECT " +
            "COALESCE(SUM(m.impressions), 0) AS totalImpressions, " +
            "COALESCE(SUM(m.clicks), 0) AS totalClicks, " +
            "COALESCE(SUM(m.conversions), 0) AS totalConversions, " +
            "COALESCE(SUM(m.spend), 0) AS totalSpend, " +
            "COALESCE(SUM(m.revenue), 0) AS totalRevenue " +
            "FROM MetricFact m " +
            "JOIN m.project p " +
            "JOIN m.adContent ac " +
            "JOIN ac.adGroup ag " +
            "JOIN ag.adCampaign camp " +
            "WHERE p.organization.id = :orgId " +
            "AND m.provider = :provider " +
            "AND camp.status = :status " +
            "AND m.timeBucket >= :startDate AND m.timeBucket < :endDate")
    MetricSumProjection findMetricsSumByOrgIdAndProvider(
            @Param("orgId") Long orgId,
            @Param("provider") Provider provider,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Status status
    );
}
