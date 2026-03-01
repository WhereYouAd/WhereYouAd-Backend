package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {
    void findAllByProvider(String provider);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId")
    Long sumAllBudgetsByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND o.id = :orgId AND c.provider = :provider")
    Long sumBudgetsByUserIdAndOrgIdAndProvider(@Param("userId") Long userId, @Param("orgId") Long orgId,
            @Param("provider") Provider provider);
}
