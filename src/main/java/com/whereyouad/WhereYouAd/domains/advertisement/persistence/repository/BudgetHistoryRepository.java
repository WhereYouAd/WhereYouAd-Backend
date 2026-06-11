package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.BudgetHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetHistoryRepository extends JpaRepository<BudgetHistory, Long> {
}
