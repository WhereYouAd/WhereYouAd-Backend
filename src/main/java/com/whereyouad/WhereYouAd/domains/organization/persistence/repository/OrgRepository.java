package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrgRepository extends JpaRepository<Organization, Long> {

    //검색하려는 문자열이 포함된 name 을 가진 조직을 추출
    @Query("select o from Organization o where o.name like concat('%', :name, '%') AND o.status = 'ACTIVE'")
    Page<Organization> findOrganizationsByName(@Param(value = "name") String name, Pageable pageable);
}
