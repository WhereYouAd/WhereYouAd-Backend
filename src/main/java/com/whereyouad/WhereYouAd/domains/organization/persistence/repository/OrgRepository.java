package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrgRepository extends JpaRepository<Organization, Long> {

    // 특정 User 가 owner 이고, 지정된 상태인 Organization 모두 조회 (User Hard Delete 정리용)
    @Query("SELECT o FROM Organization o WHERE o.ownerUserId = :ownerUserId AND o.status = :status")
    List<Organization> findAllByOwnerUserIdAndStatus(@Param("ownerUserId") Long ownerUserId,
                                                     @Param("status") OrgStatus status);
}