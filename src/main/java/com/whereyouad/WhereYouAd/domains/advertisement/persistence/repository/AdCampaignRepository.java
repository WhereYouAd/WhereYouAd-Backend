package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {
    void findAllByProvider(String provider);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND c.status = 'ON_GOING'")
    Long sumAllBudgetsByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("""
            SELECT new com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse$OngoingPlatformAdCount(
                c.provider, COUNT(c)
            )
            FROM AdCampaign c
            JOIN c.project p
            JOIN p.organization o
            WHERE o.id = :orgId
              AND c.status = :status
              AND c.startDate <= :endDate
              AND c.endDate >= :startDate
            GROUP BY c.provider
            """)
    List<DashboardResponse.OngoingPlatformAdCount> countOngoingAdsByProvider(
            @Param("orgId") Long orgId,
            @Param("status") Status status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND c.provider = :provider AND c.status = 'ON_GOING'")
    Long sumBudgetsByUserIdAndOrgIdAndProvider(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("provider") Provider provider);

    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$CampaignSummary(c.project.id, c.provider, c.budget) " +
            "FROM AdCampaign c WHERE c.project.id IN :projectIds")
    List<ProjectQueryDto.CampaignSummary> findCampaignSummariesByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT new com.whereyouad.WhereYouAd.domains.project.application.dto.ProjectQueryDto$CampaignSummary(c.project.id, c.provider, c.budget) " +
            "FROM AdCampaign c WHERE c.project.id = :projectIds")
    List<ProjectQueryDto.CampaignSummary> findCampaignSummariesByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query("UPDATE AdCampaign a SET a.status = :status WHERE a.project.id = :projectId")
    void updateStatusByProjectId(@Param("projectId") Long projectId, @Param("status") Status status);

    @Modifying
    @Query("UPDATE AdCampaign a SET a.status = :status WHERE a.project.organization.id = :orgId")
    void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);
}
