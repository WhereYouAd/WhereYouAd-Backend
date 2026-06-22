package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.BudgetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BudgetHistoryRepository extends JpaRepository<BudgetHistory, Long> {

    @Query("""
        SELECT bh FROM BudgetHistory bh
        LEFT JOIN bh.adCampaign ac
        LEFT JOIN bh.adGroup ag
        LEFT JOIN ag.adCampaign agc
        WHERE (ac.organization.id = :orgId OR agc.organization.id = :orgId)
          AND bh.createdAt BETWEEN :start AND :end
        ORDER BY bh.createdAt DESC
    """)
    List<BudgetHistory> findByOrgAndPeriod(
            @Param("orgId") Long orgId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
