package com.whereyouad.WhereYouAd.domains.timeline.persistence.repository;

import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineRepository extends JpaRepository<Timeline, Long> {
}
