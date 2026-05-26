package com.whereyouad.WhereYouAd.domains.ai.persistence.repository;

import com.whereyouad.WhereYouAd.domains.ai.persistence.entity.AIInsightReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AIInsightReportRepository extends JpaRepository<AIInsightReport, Long> {

    Optional<AIInsightReport> findByAccessToken(String accessToken);

    // 조직 ID 기준 일괄 삭제 (회원 탈퇴 정리용)
    @Modifying
    @Query("DELETE FROM AIInsightReport r WHERE r.organization.id = :orgId")
    void deleteByOrganizationId(@Param("orgId") Long orgId);
}