package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdGroupRepository extends JpaRepository<AdGroup, Long> {

    @Modifying
    @Query("UPDATE AdGroup a SET a.status = :status WHERE a.adCampaign.project.id = :projectId")
    void updateStatusByProjectId(@Param("projectId") Long projectId, @Param("status") Status status);

    @Modifying
    @Query("UPDATE AdGroup a SET a.status = :status WHERE a.adCampaign.project.organization.id = :orgId")
    void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);

    // externalGroupId로 기존 광고그룹 조회 (Meta UPSERT 용)
    Optional<AdGroup> findByExternalGroupId(String externalGroupId);
}
