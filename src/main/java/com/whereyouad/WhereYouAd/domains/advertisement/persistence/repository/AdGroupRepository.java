package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdGroupRepository extends JpaRepository<AdGroup, Long> {
}
