package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.MetricFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricFactRepository extends JpaRepository<MetricFact, Long> {
}
