package com.whereyouad.WhereYouAd.domains.organization.presentation;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgQueryService;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.presentation.docs.OrgControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/org")
public class OrgController implements OrgControllerDocs {

    private final OrgService orgService;
    private final OrgQueryService orgQueryService;

    @PostMapping("/create")
    public ResponseEntity<DataResponse<OrgResponse.Create>> createOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestPart(value = "request") @Valid OrgRequest.Create request,
            @RequestPart(value = "image", required = false) MultipartFile image
    )
    {
        OrgResponse.Create response = orgService.createOrganization(userId, request, image);
        return ResponseEntity.ok(
                DataResponse.created(response)
        );
    }

    @GetMapping("/my")
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getMyOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId
    )
    {
        OrgResponse.MyOrganizations response = orgService.getMyOrganizations(userId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @PostMapping("/{orgId}/workspace")
    public ResponseEntity<DataResponse<OrgResponse.CurrentWorkSpace>> setCurrentWorkSpace(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId)
    {
        OrgResponse.CurrentWorkSpace response = orgService.setCurrentWorkSpace(userId, orgId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @GetMapping("/my/workspace")
    public ResponseEntity<DataResponse<OrgResponse.CurrentWorkSpace>> getCurrentWorkSpace(
            @AuthenticationPrincipal(expression = "userId") Long userId)
    {
        OrgResponse.CurrentWorkSpace response = orgService.getCurrentWorkSpace(userId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @GetMapping("/{orgId}")
    public ResponseEntity<DataResponse<OrgResponse.OrgDetail>> getOrganizationDetail(@PathVariable Long orgId)
    {
        OrgResponse.OrgDetail response = orgService.getOrganizationDetail(orgId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @GetMapping("/deleted")
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getSoftDeletedOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId
    )
    {
        OrgResponse.MyOrganizations response = orgService.getSoftDeletedOrgs(userId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }


    @PatchMapping("/{orgId}")
    public ResponseEntity<DataResponse<OrgResponse.Update>> modifyOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestPart(value = "request") @Valid OrgRequest.Update request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    )
    {
        OrgResponse.Update response = orgService.modifyOrganization(userId, orgId, request, imageFile);
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @PatchMapping("/{orgId}/restore")
    public ResponseEntity<DataResponse<OrgResponse.Delete>> restoreOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        OrgResponse.Delete response = orgService.restoreOrganization(userId, orgId);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @DeleteMapping("/{orgId}")
    public ResponseEntity<DataResponse<String>> removeOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "false") boolean isHard
    )
    {
        if (isHard) { //true 로 하여 Hard Delete 시
            orgService.removeOrganization(userId, orgId); //Hard Delete
        } else { //기본값(false) 이면
            orgService.removeOrganizationSoft(userId, orgId); //Soft Delete
        }

        return ResponseEntity.ok(
                DataResponse.from("조직이 정상적으로 삭제 처리 되었습니다.")
        );
    }

    @GetMapping("/members/{orgId}")
    public ResponseEntity<DataResponse<OrgResponse.OrgMemberSliceDTO>> getOrgMembers(
            @PathVariable Long orgId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        OrgResponse.OrgMemberSliceDTO response = orgQueryService.getOrgMembers(orgId, cursor, size);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/members/{orgId}/count")
    public ResponseEntity<DataResponse<OrgResponse.OrgMemberCountDTO>> getOrgMembersCount(
            @PathVariable Long orgId
    ) {
        OrgResponse.OrgMemberCountDTO response = orgQueryService.getOrgMembersCount(orgId);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @DeleteMapping("{orgId}/members/{memberId}")
    public ResponseEntity<DataResponse<String>> removeMember(
            @AuthenticationPrincipal(expression = "userId") Long userId, // 관리자 ID
            @PathVariable Long orgId,
            @PathVariable Long memberId
    ) {
        orgService.removeMemberFromOrg(userId, orgId, memberId);
        return ResponseEntity.ok(DataResponse.from("해당 맴버가 조직에서 제외되었습니다."));
    }

    @PatchMapping("/members/{orgId}/{memberId}")
    public ResponseEntity<DataResponse<OrgResponse.OrgMemberDTO>> updateOrgMembersRole(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @RequestBody @Valid OrgRequest.UpdateRole dto
    ) {
        OrgResponse.OrgMemberDTO response = orgService.updateOrgMembersRole(userId, orgId, memberId, dto);
        return ResponseEntity.ok(DataResponse.from(response));
    }

    @PostMapping("/members/{orgId}/invitation")
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> sendOrgInvitation(
            @AuthenticationPrincipal(expression = "userId") Long userId, @PathVariable Long orgId,
            @RequestBody @Valid OrgRequest.Invite request) {
        OrgResponse.OrgInvitationResponse orgInvitationResponse = orgService.sendOrgInvitation(userId, orgId,
                request.email());
        return ResponseEntity.ok(DataResponse.from(orgInvitationResponse));
    }

    @PostMapping("/invitations/{token}")
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> acceptOrgInvitation(
            @AuthenticationPrincipal(expression = "userId") Long userId, @PathVariable String token) {
        OrgResponse.OrgInvitationResponse orgInvitationResponse = orgService.acceptOrgInvitation(userId, token);
        return ResponseEntity.ok(DataResponse.from(orgInvitationResponse));
    }
}