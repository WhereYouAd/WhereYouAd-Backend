package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.project.persistence.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    // 지정된 기간 동안의 projects 에 대한 모든 지표 합산
    @Query("SELECT " +
            "COALESCE(SUM(m.impressions), 0) AS totalImpressions, " +
            "COALESCE(SUM(m.clicks), 0) AS totalClicks, " +
            "COALESCE(SUM(m.conversions), 0) AS totalConversions, " +
            "COALESCE(SUM(m.spend), 0) AS totalSpend, " +
            "COALESCE(SUM(m.revenue), 0) AS totalRevenue " +
            "FROM MetricFact m " +
            "WHERE m.project IN :projects " +
            "AND m.timeBucket BETWEEN :startDate AND :endDate")
    MetricSumProjection findMetricsSumByProjectsAndDateRange(
            @Param("projects") List<Project> projects,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    //orgId 와 provider 가 일치하는 지표에 대해 합산
    @Query("SELECT " +
            "SUM(m.impressions) AS totalImpressions, " +
            "SUM(m.clicks) AS totalClicks, " +
            "SUM(m.conversions) AS totalConversions, " +
            "SUM(m.spend) AS totalSpend, " +
            "SUM(m.revenue) AS totalRevenue " +
            "FROM MetricFact m " +
            "JOIN m.project p " +
            "WHERE p.organization.id = :orgId " +
            "AND m.provider = :provider " +
            "AND m.timeBucket BETWEEN :startDate AND :endDate")
    MetricSumProjection findMetricsSumByOrgIdAndProvider(
            @Param("orgId") Long orgId,
            @Param("provider") Provider provider,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
