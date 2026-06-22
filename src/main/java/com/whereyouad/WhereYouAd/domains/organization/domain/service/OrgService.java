package com.whereyouad.WhereYouAd.domains.organization.domain.service;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OrgService {

    OrgResponse.Create createOrganization(Long userId, OrgRequest.Create request, MultipartFile imageFile);

    OrgResponse.MyOrganizations getMyOrganizations(Long userId);

    OrgResponse.CurrentWorkspace setCurrentWorkspace(Long userId, Long orgId);

    OrgResponse.CurrentWorkspace getCurrentWorkspace(Long userId);

    OrgResponse.OrgDetail getOrganizationDetail(Long orgId);

    OrgResponse.MyOrganizations getSoftDeletedOrgs(Long userId);

    OrgResponse.Update modifyOrganization(Long userId, Long orgId, OrgRequest.Update request, MultipartFile imageFile);

    void removeOrganization(Long userId, Long orgId);

    void removeOrganizationSoft(Long userId, Long orgId);

    // 회원 탈퇴 전용 Soft Delete - 플랫폼 연동 검증 생략 (연동 해제는 UserDeleteScheduler 가 Hard Delete 시점에 처리)
    void removeOrganizationSoftForWithdrawal(Long orgId);

    // User Hard Delete 정리용 - 특정 User 가 owner 인 Soft Deleted Organization 들을 관련 엔티티와 함께 Hard Delete
    void removeOrganizationsOwnedBySoftDeletedUser(Long userId);

    OrgResponse.Delete restoreOrganization(Long userId, Long orgId);

    // orgId 조직에서 memberId에 해당하는 맴버 제거
    void removeMemberFromOrg(Long userId, Long orgId, Long memberId);

    // 조직 내 멤버 권한 변경 메서드
    OrgResponse.OrgMemberDTO updateOrgMembersRole(Long userId, Long orgId, Long memberId, OrgRequest.UpdateRole dto);

    OrgResponse.OrgInvitationResponse sendOrgInvitation(Long userId, Long orgId, String email);

    OrgResponse.OrgInvitationResponse acceptOrgInvitation(Long userId, String token);

    void changeOwner(Long userId, Long orgId, OrgRequest.ChangeOwner request);
}
