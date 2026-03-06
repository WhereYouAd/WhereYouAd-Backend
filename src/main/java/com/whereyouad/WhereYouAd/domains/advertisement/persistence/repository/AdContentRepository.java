package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdContentRepository extends JpaRepository<AdContent, Long> {
}
