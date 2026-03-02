package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
