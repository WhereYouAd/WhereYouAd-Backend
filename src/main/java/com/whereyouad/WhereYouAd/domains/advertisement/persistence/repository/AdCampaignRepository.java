package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {
    void findAllByProvider(String provider);

    @Query("SELECT SUM(c.budget) FROM AdCampaign c")
    Long sumAllBudgets();

    @Query("SELECT SUM(c.budget) FROM AdCampaign c WHERE c.provider = :provider")
    Long sumBudgetsByProvider(@Param("provider") Provider provider);
}
