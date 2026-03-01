package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {
    void findAllByProvider(String provider);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId")
    Long sumAllBudgetsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c JOIN c.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND c.provider = :provider")
    Long sumBudgetsByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") Provider provider);
}
