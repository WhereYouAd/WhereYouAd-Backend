package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdCampaign;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;
import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdGroupRepository extends JpaRepository<AdGroup, Long> {

    // 플랫폼 계정이랑 그룹 ID가 일치하는 광고 그룹 단건 조회
    @Query("SELECT g FROM AdGroup g WHERE g.externalGroupId = :externalGroupId AND g.adCampaign.platformAccount = :platformAccount")
    Optional<AdGroup> findByExternalGroupIdAndPlatformAccount(@Param("externalGroupId") String externalGroupId, @Param("platformAccount") PlatformAccount platformAccount);

    @Modifying
    @Query("UPDATE AdGroup a SET a.status = :status WHERE a.adCampaign.project.id = :projectId")
    void updateStatusByProjectId(@Param("projectId") Long projectId, @Param("status") Status status);

    @Modifying
    @Query("UPDATE AdGroup a SET a.status = :status WHERE a.adCampaign.project.organization.id = :orgId")
    void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);

    // externalGroupId + 부모 캠페인으로 기존 광고그룹 조회 (Meta UPSERT 용 — 계정 간 ID 충돌 방지)
    Optional<AdGroup> findByExternalGroupIdAndAdCampaign(String externalGroupId, AdCampaign adCampaign);

    Optional<AdGroup> findByAdCampaignAndExternalGroupId(AdCampaign adCampaign, String externalGroupId);

    Optional<AdGroup> findByAdCampaign_PlatformAccountAndExternalGroupId(PlatformAccount platformAccount, String externalGroupId);
}
