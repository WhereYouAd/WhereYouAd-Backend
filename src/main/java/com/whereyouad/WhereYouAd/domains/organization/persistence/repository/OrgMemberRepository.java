package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.UserStatus;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {

    //User 가 가진 OrgMember 모두 추출하는 메서드
    List<OrgMember> findOrgMemberByUser(User user);
    
    //userId 를 통해 OrgMember 추출 -> Organization 의 status 가 ACTIVE 인 경우에만 조회
    @Query(value = "select om from OrgMember om join fetch om.organization o where om.user.id = :userId and o.status = 'ACTIVE'")
    List<OrgMember> findOrgMemberByUserId(@Param("userId") Long userId);

    //userId 를 통해 OrgMember 추출 -> Organization 의 status 가 DELETED 인 경우에만 조회
    @Query(value = "select om from OrgMember om join fetch om.organization o where om.user.id = :userId and o.status = 'DELETED'")
    List<OrgMember> findOrgMemberByUserIdSoftDeleted(@Param("userId") Long userId);

    //특정 Organization 에 속한 OrgMember 모두 추출하는 메서드
    @Query("select om from OrgMember om where om.organization = :organization")
    List<OrgMember> findOrgMemberByOrg(@Param(value = "organization") Organization organization);

    // 조직 멤버 조회 (무한 스크롤 - Slice 반환)
    @Query("SELECT m FROM OrgMember m " +
            "JOIN FETCH m.user u " +
            "WHERE m.organization.id = :orgId " +
            "AND u.status = :status " +
            "AND (:cursor IS NULL OR m.id > :cursor) " +
            "ORDER BY m.id ASC")
    Slice<OrgMember> findByOrganizationIdWithCursor(
            @Param("orgId") Long orgId,
            @Param("status") UserStatus status,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // userId 와 orgId 로 특정 OrgMember 조회
    @Query("SELECT om FROM OrgMember om " +
            "WHERE om.user.id = :userId " +
            "AND om.organization.id = :orgId " +
            "AND om.user.status = 'ACTIVE'")
    Optional<OrgMember> findByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    // 조직의 전체 멤버 수 조회
    @Query("SELECT COUNT(m) FROM OrgMember m " +
            "JOIN m.user u " +
            "WHERE m.organization.id = :orgId " +
            "AND u.status = :status")
    int countByOrganizationIdAndUserStatus(
            @Param("orgId") Long orgId,
            @Param("status") UserStatus status
    );

        Boolean existsByUserAndOrganization(User user, Organization organization);

    // 조직 id에 해당하는 역할 인원 수 조회
    long countByOrganizationIdAndRole(Long orgId, OrgRole orgRole);

    // 유저가 조직에 속하는지 확인
    boolean existsByUserIdAndOrganizationId(Long userId, Long orgId);
}