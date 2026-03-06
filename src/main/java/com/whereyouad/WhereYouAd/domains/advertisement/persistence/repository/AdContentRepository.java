package com.whereyouad.WhereYouAd.domains.advertisement.persistence.repository;

import com.whereyouad.WhereYouAd.domains.advertisement.persistence.entity.AdContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdContentRepository extends JpaRepository<AdContent, Long> {

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
}
