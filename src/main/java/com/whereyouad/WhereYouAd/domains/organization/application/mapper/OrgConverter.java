package com.whereyouad.WhereYouAd.domains.organization.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgRole;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgMember;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.Organization;

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

    public static OrgResponse.MyOrganizations toMyOrganizations(List<OrgResponse.SimpleInfo> infos) {
        return new OrgResponse.MyOrganizations(
                infos
        );
    }

    public static OrgResponse.OrgDetail toOrgDetail(Organization organization) {
        return new OrgResponse.OrgDetail(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                organization.getCreatedAt()
        );
    }

    public static OrgResponse.SimpleInfo toOrgSimpleInfo(OrgMember orgMember) {
        //OrgMember 내부에 존재하는 Role 활용
        Organization organization = orgMember.getOrganization();

        return new OrgResponse.SimpleInfo(organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                orgMember.getRole()
        );
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

    // 조직 멤버 Slice DTO 변환 (무한 스크롤)
    public static OrgResponse.OrgMemberSliceDTO toOrgMemberSliceDTO(
            boolean hasNext,
            String nextCursor,
            List<OrgMember> orgMembers
    ) {
        List<OrgResponse.OrgMemberDTO> memberDTOs = orgMembers.stream()
                .map(m -> new OrgResponse.OrgMemberDTO(
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getUser().getProfileImageUrl(),
                        m.getRole().name()
                ))
                .toList();

        return new OrgResponse.OrgMemberSliceDTO(
                hasNext,
                nextCursor,
                memberDTOs
        );
    }
}
