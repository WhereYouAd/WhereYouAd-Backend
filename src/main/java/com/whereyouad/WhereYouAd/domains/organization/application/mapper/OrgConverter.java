package com.whereyouad.WhereYouAd.domains.organization.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

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
