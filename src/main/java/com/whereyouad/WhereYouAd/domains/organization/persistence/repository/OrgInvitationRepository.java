package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgInvitation;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrgInvitationRepository extends JpaRepository<OrgInvitation, Long> {

    // 조직 ID로 유효한(만료되지 않은) 초대 목록 조회
    List<OrgInvitation> findByOrganizationIdAndExpireAtAfter(Long orgId, LocalDateTime now);

    // email과 조직으로 초대 맴버 조회
    Optional<OrgInvitation> findByEmailAndOrganization(String email, Organization organization);

    // 만료일 기준 삭제
    @Modifying
    @Query("DELETE FROM OrgInvitation ov WHERE ov.expireAt < :now")
    void deleteByExpireAtBefore(@Param("now") LocalDateTime now);

    // 조직 ID 기준 일괄 삭제 (회원 탈퇴 정리용)
    @Modifying
    @Query("DELETE FROM OrgInvitation ov WHERE ov.organization.id = :orgId")
    void deleteByOrganizationId(@Param("orgId") Long orgId);

    // 이메일 기준 일괄 삭제 (탈퇴 회원 앞으로 발송된 pending 초대 정리용)
    @Modifying
    @Query("DELETE FROM OrgInvitation ov WHERE ov.email = :email")
    void deleteByEmail(@Param("email") String email);
}
