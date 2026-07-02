package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Status;

import com.whereyouad.WhereYouAd.domains.platform.persistence.entity.PlatformAccount;

import java.util.List;
import java.util.Optional;

public interface AdContentRepository extends JpaRepository<AdContent, Long> {

        // 플랫폼 계정에 속하고 외부 광고 ID가 일치하는 광고 단건 조회
        @Query("SELECT c FROM AdContent c WHERE c.externalAdId = :externalAdId AND c.adGroup.adCampaign.platformAccount = :platformAccount")
        Optional<AdContent> findByExternalAdIdAndPlatformAccount(@Param("externalAdId") String externalAdId, @Param("platformAccount") PlatformAccount platformAccount);

        // 플랫폼 계정에 속한 모든 광고 목록을 조회
        @Query("SELECT ac FROM AdContent ac JOIN FETCH ac.adGroup ag JOIN FETCH ag.adCampaign c WHERE c.platformAccount = :platformAccount")
        List<AdContent> findAllByPlatformAccount(@Param("platformAccount") PlatformAccount platformAccount);

        @Query("SELECT ac FROM AdContent ac " +
                        "JOIN FETCH ac.adGroup ag " +
                        "JOIN ag.adCampaign c " +
                        "JOIN c.project p " +
                        "JOIN p.organization o " +
                        "WHERE ac.id = :adContentId " +
                        "AND p.id = :projectId " +
                        "AND o.id = :orgId")
        Optional<AdContent> findByIdWithValidation(
                        @Param("adContentId") Long adContentId,
                        @Param("projectId") Long projectId,
                        @Param("orgId") Long orgId);

        @Query("SELECT ac FROM AdContent ac " +
                        "JOIN ac.adGroup ag " +
                        "JOIN ag.adCampaign c " +
                        "JOIN c.project p " +
                        "JOIN p.organization o " +
                        "WHERE ac.id = :adContentId " +
                        "AND o.id = :orgId")
        Optional<AdContent> findByIdAndOrganizationId(
                        @Param("adContentId") Long adContentId,
                        @Param("orgId") Long orgId);

        @Query("SELECT ac FROM AdContent ac " +
                        "JOIN FETCH ac.adGroup ag " +
                        "JOIN ag.adCampaign c " +
                        "JOIN c.project p " +
                        "JOIN p.organization o " +
                        "WHERE p.id = :projectId " +
                        "AND o.id = :orgId")
        List<AdContent> findAllByIdWithValidation(@Param("projectId") Long projectId, @Param("orgId") Long orgId);

        @Modifying
        @Query("UPDATE AdContent a SET a.status = :status WHERE a.adGroup.adCampaign.project.id = :projectId")
        void updateStatusByProjectId(@Param("projectId") Long projectId, @Param("status") Status status);

        @Modifying
        @Query("UPDATE AdContent a SET a.status = :status WHERE a.adGroup.adCampaign.project.organization.id = :orgId")
        void updateStatusByOrganizationId(@Param("orgId") Long orgId, @Param("status") Status status);

        // trackingUrl이 존재하는지 확인(트래킹 링크 중복 생성 방지)
        boolean existsByTrackingUrl(String trackingUrl);

        Optional<AdContent> findByTrackingUrl(String trackingUrl);

        // Fetch Join을 사용하여 AdContent, AdGroup, AdCampaign을 한 번의 쿼리로 모두 가져옴
        @Query("SELECT ac FROM AdContent ac " +
                "JOIN FETCH ac.adGroup ag " +
                "JOIN FETCH ag.adCampaign " +
                "WHERE ac.id = :id")
        Optional<AdContent> findByIdWithGroupAndCampaign(@Param("id") Long id);

        @Query("SELECT ac FROM AdContent ac " +
                "JOIN FETCH ac.adGroup ag " +
                "JOIN FETCH ag.adCampaign c " +
                "WHERE c.organization.id = :orgId")
        List<AdContent> findAllByOrganizationId(@Param("orgId") Long orgId);

        // externalAdId + 부모 광고그룹으로 기존 소재 조회 (Meta UPSERT 용 — 계정 간 ID 충돌 방지)
        Optional<AdContent> findByExternalAdIdAndAdGroup(String externalAdId, AdGroup adGroup);

        Optional<AdContent> findByAdGroupAndExternalAdId(AdGroup adGroup, String externalAdId);

        Optional<AdContent> findByAdGroup_AdCampaign_PlatformAccountAndExternalAdId(PlatformAccount platformAccount, String externalAdId);

        // 활성 상태인 모든 광고와 조직 id, 플랫폼(Provider)을 조회
        @Query("SELECT ac.id, o.id, c.provider FROM AdContent ac " +
               "JOIN ac.adGroup ag " +
               "JOIN ag.adCampaign c " +
               "JOIN c.project p " +
               "JOIN p.organization o " +
               "WHERE ac.status = :status AND ag.status = :status AND c.status = :status")
        List<Object[]> findAllActiveAdOrgMappings(@Param("status") Status status);
}
