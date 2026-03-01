package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {
    @Query("SELECT SUM(m.spend) FROM MetricFact m")
    BigDecimal sumAllSpends();

    @Query("SELECT SUM(m.spend) FROM MetricFact m WHERE m.provider = :provider")
    BigDecimal sumSpendsByProvider(@Param("provider") Provider provider);
}
