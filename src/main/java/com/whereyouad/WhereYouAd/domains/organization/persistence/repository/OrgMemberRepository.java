package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
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

}
