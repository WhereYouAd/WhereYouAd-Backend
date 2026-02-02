package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgRepository extends JpaRepository<Organization, Long> {
}
