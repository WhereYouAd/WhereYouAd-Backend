package com.whereyouad.WhereYouAd.domains.organization.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;
import com.whereyouad.WhereYouAd.domains.user.persistence.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public class OrgConverter {

    //Entity -> DTO
    public static OrgResponse.Create toCreatedResponse(Organization organization) {
        return new OrgResponse.Create(organization.getId(), organization.getCreatedAt());
    }

    public static OrgResponse.Update toUpdatedResponse(Organization organization) {
        return new OrgResponse.Update(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                organization.getUpdatedAt());
    }

    public static OrgResponse.Delete toRestoredResponse(Organization organization) {
        return new OrgResponse.Delete(organization.getId(),
                "해당 조직이 활성화 되었습니다");
    }

    public static OrgResponse.ListInfo toListInfo(Organization organization) {
        return new OrgResponse.ListInfo(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl()
        );
    }

    public static OrgResponse.MyOrganizations toMyOrganizations(Page<OrgResponse.SimpleInfo> page) {
        return new OrgResponse.MyOrganizations(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    public static OrgResponse.OrgSearchList toOrgSearchList(String query, Page<OrgResponse.ListInfo> page) {
        return new OrgResponse.OrgSearchList(
                query,
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    public static OrgResponse.OrgMembers toOrgMembers(User user, Organization organization) {
        OrgRole role;

        if (organization.getOwnerUserId().equals(user.getId())) {
            role = OrgRole.ADMIN;
        } else {
            role = OrgRole.MEMBER;
        }
        return new OrgResponse.OrgMembers(user.getId(), user.getName(), user.getEmail(), role);
    }

    public static OrgResponse.OrgDetail toOrgDetail(Organization organization, List<OrgResponse.OrgMembers> orgMembers) {
        return new OrgResponse.OrgDetail(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                organization.getCreatedAt(),
                orgMembers
        );
    }

    public static OrgResponse.SimpleInfo toOrgSimpleInfo(Organization organization, Long userId) {
        OrgRole myRole;

        if (organization.getOwnerUserId().equals(userId)) {
            myRole = OrgRole.ADMIN;
        } else {
            myRole = OrgRole.MEMBER;
        }

        return new OrgResponse.SimpleInfo(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                myRole);
    }

    //DTO -> Entity
    public static Organization toOrganization(Long userId, OrgRequest.Create request) {
        return Organization.builder()
                .name(request.name())
                .description(request.description())
                .logoUrl(request.logoUrl())
                .ownerUserId(userId)
                .status(OrgStatus.ACTIVE)
                .build();
    }
}
