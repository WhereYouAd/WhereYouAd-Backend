package com.whereyouad.WhereYouAd.domains.platform.persistence.repository;

import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConnectionRepository extends JpaRepository<PlatformConnection, Long> {
}
