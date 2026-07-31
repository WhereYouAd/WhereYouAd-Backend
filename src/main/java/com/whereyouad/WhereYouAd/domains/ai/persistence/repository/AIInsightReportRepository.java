package com.whereyouad.WhereYouAd.domains.ai.persistence.repository;

import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import com.whereyouad.WhereYouAd.domains.ai.persistence.repository.projection.AIReportSummaryProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AIInsightReportRepository extends JpaRepository<AIInsightReport, Long> {

    Optional<AIInsightReport> findByAccessToken(String accessToken);

    @Query("""
            SELECT r.id AS reportId,
                   r.accessToken AS accessToken,
                   r.periodStart AS periodStart,
                   r.periodEnd AS periodEnd,
                   r.status AS status,
                   r.reportType AS reportType,
                   r.isShared AS shared,
                   r.createdAt AS createdAt
            FROM AIInsightReport r
            WHERE r.organization.id = :orgId
              AND (:reportType IS NULL OR UPPER(r.reportType) = :reportType)
              AND (:cursor IS NULL OR r.id < :cursor)
            ORDER BY r.id DESC
            """)
    Slice<AIReportSummaryProjection> findSummariesByOrganizationId(
            @Param("orgId") Long orgId,
            @Param("reportType") String reportType,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // 조직 ID 기준 일괄 삭제 (회원 탈퇴 정리용)
    @Modifying
    @Query("DELETE FROM AIInsightReport r WHERE r.organization.id = :orgId")
    void deleteByOrganizationId(@Param("orgId") Long orgId);
}
