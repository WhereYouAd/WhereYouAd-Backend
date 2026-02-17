package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {

    //User 가 가진 OrgMember 모두 추출하는 메서드
    List<OrgMember> findOrgMemberByUser(User user);

    //특정 Organization 에 속한 OrgMember 모두 추출하는 메서드
    @Query("select om from OrgMember om where om.organization = :organization")
    List<OrgMember> findOrgMemberByOrg(@Param(value = "organization") Organization organization);

    // 조직 멤버 조회 (무한 스크롤 - Slice 반환)
    @Query("SELECT m FROM OrgMember m " +
            "JOIN FETCH m.user " +
            "WHERE m.organization.id = :orgId " +
            "AND (:cursor IS NULL OR m.id > :cursor) " +
            "ORDER BY m.id ASC")
    Slice<OrgMember> findByOrganizationIdWithCursor(
            @Param("orgId") Long orgId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // 조직의 전체 멤버 수 조회
    int countByOrganizationId(Long orgId);
}