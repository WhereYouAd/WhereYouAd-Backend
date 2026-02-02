package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {
}
