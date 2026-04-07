package com.whereyouad.WhereYouAd.domains.organization.application.mapper;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.constant.OrgStatus;
import com.whereyouad.WhereYouAd.domains.organization.persistence.entity.OrgInvitation;
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
        return toOrgSimpleInfo(orgMember, null);
    }

    public static OrgResponse.SimpleInfo toOrgSimpleInfo(OrgMember orgMember, Long currentOrgId) {
        Organization organization = orgMember.getOrganization();
        boolean isCurrentWorkSpace = currentOrgId != null && currentOrgId.equals(organization.getId());

        return new OrgResponse.SimpleInfo(
                organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getLogoUrl(),
                orgMember.getRole(),
                isCurrentWorkSpace
        );
    }

    //DTO -> Entity
    public static Organization toOrganization(Long userId, OrgRequest.Create request, String imageUrl) {
        return Organization.builder()
                .name(request.name())
                .description(request.description())
                .logoUrl(imageUrl)
                .ownerUserId(userId)
                .status(OrgStatus.ACTIVE)
                .build();
    }

    // 단일 OrgMember -> OrgMemberDTO 변환
    public static OrgResponse.OrgMemberDTO toOrgMemberDTO(OrgMember orgMember) {
        return new OrgResponse.OrgMemberDTO(
                orgMember.getUser().getId(),
                orgMember.getUser().getName(),
                orgMember.getUser().getEmail(),
                orgMember.getUser().getProfileImageUrl(),
                orgMember.getRole().name()
        );
    }

    // 조직 멤버 Slice DTO 변환 (무한 스크롤)
    public static OrgResponse.OrgMemberSliceDTO toOrgMemberSliceDTO(
            boolean hasNext,
            String nextCursor,
            List<OrgMember> orgMembers
    ) {
        List<OrgResponse.OrgMemberDTO> memberDTOs = orgMembers.stream()
                .map(OrgConverter::toOrgMemberDTO)
                .toList();

        return new OrgResponse.OrgMemberSliceDTO(
                hasNext,
                nextCursor,
                memberDTOs
        );
    }

    // dto -> entity
    public static OrgInvitation toOrgInvitation(String email, Organization organization) {
        return OrgInvitation.builder()
                .email(email)
                .organization(organization)
                .invitedAt(java.time.LocalDateTime.now())
                .expireAt(java.time.LocalDateTime.now().plusHours(24))
                .build();
    }

    // 단일 OrgInvitation -> OrgPendingMemberDTO 변환
    public static OrgResponse.OrgPendingMemberDTO toOrgPendingMemberDTO(OrgInvitation invitation) {
        return new OrgResponse.OrgPendingMemberDTO(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getInvitedAt(),
                invitation.getExpireAt()
        );
    }

    // List<OrgInvitation> -> OrgPendingMembersResponse 변환
    public static OrgResponse.OrgPendingMembersResponse toOrgPendingMembersResponse(List<OrgInvitation> invitations) {
        List<OrgResponse.OrgPendingMemberDTO> dtos = invitations.stream()
                .map(OrgConverter::toOrgPendingMemberDTO)
                .toList();
        return new OrgResponse.OrgPendingMembersResponse(dtos);
    }
}