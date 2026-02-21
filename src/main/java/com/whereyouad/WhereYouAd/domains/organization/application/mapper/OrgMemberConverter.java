package com.whereyouad.WhereYouAd.domains.organization.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;

import java.time.LocalDateTime;

public class OrgMemberConverter {

    public static OrgMember toOrgMemberADMIN(User user, Organization organization) {
        return OrgMember.builder()
                .user(user)
                .organization(organization)
                .joinedAt(organization.getCreatedAt()) // 생성한 사람의 조직 합류 시간은 조직 생성 시간과 동일
                .role(OrgRole.ADMIN) // 생성한 사람은 ADMIN
                .build();
    }

    public static OrgMember toOrgMemberMEMBER(User user, Organization organization) {
        return OrgMember.builder()
                .user(user)
                .organization(organization)
                .joinedAt(LocalDateTime.now()) // 조직 초대 완료 시점
                .role(OrgRole.MEMBER) // 초대된 사람은 MEMBER (default)
                .build();
    }
}
