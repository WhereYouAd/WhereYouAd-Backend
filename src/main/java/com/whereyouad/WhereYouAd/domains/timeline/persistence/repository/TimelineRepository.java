package com.whereyouad.WhereYouAd.domains.timeline.persistence.repository;

import com.whereyouad.WhereYouAd.domains.timeline.persistence.entity.Timeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimelineRepository extends JpaRepository<Timeline, Long> {

    // 조직 ID 기준 일괄 삭제 (회원 탈퇴 정리용)
    @Modifying
    @Query("DELETE FROM Timeline t WHERE t.organization.id = :orgId")
    void deleteByOrganizationId(@Param("orgId") Long orgId);
}
