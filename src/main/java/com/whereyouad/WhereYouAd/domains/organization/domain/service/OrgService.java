package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OrgService {

    OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request, MultipartFile imageFile);

    OrgResponse.MyOrganizations getMyOrganizations(Long userId);

    OrgResponse.CurrentWorkSpace setCurrentWorkSpace(Long userId, Long orgId);

    OrgResponse.OrgDetail getOrganizationDetail(Long orgId);

    OrgResponse.MyOrganizations getSoftDeletedOrgs(Long userId);

    OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request, MultipartFile imageFile);

    void removeOrganization(Long userId, Long orgId);

    void removeOrganizationSoft(Long userId, Long orgId);

    OrgResponse.Delete restoreOrganization(Long userId, Long orgId);

    // orgId 조직에서 memberId에 해당하는 맴버 제거
    void removeMemberFromOrg(Long userId, Long orgId, Long memberId);

    // 조직 내 멤버 권한 변경 메서드
    OrgResponse.OrgMemberDTO updateOrgMembersRole(Long userId, Long orgId, Long memberId, OrgRequest.UpdateRole dto);

    OrgResponse.OrgInvitationResponse sendOrgInvitation(Long userId, Long orgId, String email);

    OrgResponse.OrgInvitationResponse acceptOrgInvitation(Long userId, String token);
}
