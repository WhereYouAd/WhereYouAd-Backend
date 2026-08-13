package com.whereyouad.WhereYouAd.domains.click.persistence.repository;

import com.whereyouad.WhereYouAd.domains.click.persistence.entity.ClickAnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickAnomalyEventRepository extends JpaRepository<ClickAnomalyEvent, Long> {
}
