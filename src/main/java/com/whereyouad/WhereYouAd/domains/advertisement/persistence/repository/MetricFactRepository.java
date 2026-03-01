package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {
    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId")
    BigDecimal sumAllSpendsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(m.spend) FROM MetricFact m JOIN m.project p JOIN p.organization o JOIN OrgMember om ON om.organization = o WHERE om.user.id = :userId AND m.provider = :provider")
    BigDecimal sumSpendsByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") Provider provider);
}
