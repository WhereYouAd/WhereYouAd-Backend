package com.whereyouad.WhereYouAd.domains.organization.persistence.repository;

import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrgMemberRepository extends JpaRepository<OrgMember, Long> {

    //User 가 가진 OrgMember 모두 추출하는 메서드
    List<OrgMember> findOrgMemberByUser(User user);

    //userId 를 통해 OrgMember 추출 -> Organization 의 status 가 ACTIVE 인 경우에만 조회
    @Query(value = "select om from OrgMember om join fetch om.organization o where om.user.id = :userId and o.status = 'ACTIVE'")
    List<OrgMember> findOrgMemberByUserId(@Param("userId") Long userId);

    //특정 Organization 에 속한 OrgMember 모두 추출하는 메서드
    @Query("select om from OrgMember om where om.organization = :organization")
    List<OrgMember> findOrgMemberByOrg(@Param(value = "organization") Organization organization);
}
