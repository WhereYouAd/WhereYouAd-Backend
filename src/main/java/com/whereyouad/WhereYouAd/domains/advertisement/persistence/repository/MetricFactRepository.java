package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Grain;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.MetricSumProjection;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository.projection.RoasProjection;
import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import java.util.List;

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
            "WHERE p.organization.id = :orgId " +
            "AND p.organization.status = :orgStatus " +
            "AND m.timeBucket >= :startDate AND m.timeBucket < :endDate " +
            "AND m.adContent.id IN (" +
            "   SELECT ac.id " +
            "   FROM AdContent ac " +
            "   JOIN ac.adGroup ag " +
            "   JOIN ag.adCampaign camp " +
            "   WHERE camp.status = :status" + ")"
            )
    MetricSumProjection findMetricsSumByOrgIdAndDateRange(
            @Param("orgId") Long orgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orgStatus") OrgStatus orgStatus,
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
            "WHERE p.organization.id = :orgId " +
            "AND p.organization.status = :orgStatus " +
            "AND m.provider = :provider " +
            "AND m.timeBucket >= :startDate AND m.timeBucket < :endDate " +
            "AND m.adContent.id IN (" +
            "   SELECT ac.id " +
            "   FROM AdContent ac " +
            "   JOIN ac.adGroup ag " +
            "   JOIN ag.adCampaign camp " +
            "   WHERE camp.status = :status" + ")"
            )
    MetricSumProjection findMetricsSumByOrgIdAndProvider(
            @Param("orgId") Long orgId,
            @Param("provider") Provider provider,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orgStatus") OrgStatus orgStatus,
            @Param("status") Status status
    );

    // 특정 조직(orgId)에 속한 모든 프로젝트의 지정된 기간(start~end) 동안
    // 플랫폼(provider)별 총 매출액과 총 광고비를 합산하여 RoasProjection 형태로 조회
    @Query("SELECT " +
           "mf.provider AS provider, " +
           "SUM(mf.revenue) AS totalRevenue, " +
           "SUM(mf.spend) AS totalSpend " +
           "FROM MetricFact mf " +
           "JOIN mf.project p " +
           "WHERE p.organization.id = :orgId " +
           "AND mf.timeBucket >= :start " +
           "AND mf.timeBucket <= :end " +
           "GROUP BY mf.provider")
    List<RoasProjection> findRoasByOrgAndPeriod(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // 프로젝트 ID 목록으로 지출(spend) 총합 일괄 조회
    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$SpendSummary(m.project.id, SUM(m.spend)) " +
            "FROM MetricFact m WHERE m.project.id IN :projectIds GROUP BY m.project.id")
    List<ProjectQueryDto.SpendSummary> findSpendSummariesByProjectIds(@Param("projectIds") List<Long> projectIds);

    // 특정 조직의 일정 기간 내 MetricFact 데이터 유무 확인
    @Query("SELECT COUNT(m) > 0 FROM MetricFact m " +
           "WHERE m.project.organization.id = :orgId " +
           "AND m.timeBucket >= :start " +
           "AND m.timeBucket <= :end")
    boolean existsByTimeBucketBetweenAndOrg(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orgId") Long orgId
    );

    // 특정 조직 + 플랫폼의 일정 기간 내 데이터 유무 확인
    @Query("SELECT COUNT(m) > 0 FROM MetricFact m " +
           "WHERE m.project.organization.id = :orgId " +
           "AND m.provider = :provider " +
           "AND m.timeBucket >= :start " +
           "AND m.timeBucket <= :end")
    boolean existsByTimeBucketBetweenAndOrgAndProvider(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orgId") Long orgId,
            @Param("provider") Provider provider
    );

    // 정해진 기간동안의 조직 단위 MetricFact 데이터 조회
    @Query("SELECT m FROM MetricFact m " +
           "JOIN FETCH m.adContent ac " +
           "JOIN FETCH ac.adGroup ag " +
           "JOIN FETCH ag.adCampaign camp " +
           "WHERE m.project.organization.id = :orgId " +
           "AND m.timeBucket >= :start " +
           "AND m.timeBucket <= :end " +
           "ORDER BY m.timeBucket ASC")
    List<MetricFact> findAllByDateRangeAndOrgForAiAnalysis(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orgId") Long orgId
    );

    // 정해진 기간동안의 플랫폼 단위 MetricFact 데이터 조회
    @Query("SELECT m FROM MetricFact m " +
           "JOIN FETCH m.adContent ac " +
           "JOIN FETCH ac.adGroup ag " +
           "JOIN FETCH ag.adCampaign camp " +
           "WHERE m.project.organization.id = :orgId " +
           "AND m.provider = :provider " +
           "AND m.timeBucket >= :start " +
           "AND m.timeBucket <= :end " +
           "ORDER BY m.timeBucket ASC")
    List<MetricFact> findAllByDateRangeAndOrgAndProviderForAiAnalysis(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("orgId") Long orgId,
            @Param("provider") Provider provider
    );

    // adContent + timeBucket + provider 으로 기존 지표 조회 (Meta UPSERT 중복 방지)
    Optional<MetricFact> findByAdContentAndTimeBucketAndProvider(AdContent adContent, LocalDateTime timeBucket, Provider provider);

    // Upsert 선행 조회용
    Optional<MetricFact> findByAdContentAndTimeBucketAndGrain(
            AdContent adContent,
            LocalDateTime timeBucket,
            Grain grain
    );
}