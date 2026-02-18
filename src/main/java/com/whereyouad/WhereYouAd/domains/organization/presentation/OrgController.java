package com.whereyouad.WhereYouAd.domains.organization.presentation;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.presentation.docs.OrgControllerDocs;
import com.whereyouad.WhereYouAd.domains.user.domain.service.EmailService;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/org")
public class OrgController implements OrgControllerDocs {

    private final OrgService orgService;

    @PostMapping("/create")
    public ResponseEntity<DataResponse<OrgResponse.Create>> createOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestBody @Valid OrgRequest.Create request
    ) {
        OrgResponse.Create response = orgService.createOrganization(userId, request);
        return ResponseEntity.ok(
                DataResponse.created(response)
        );
    }

    @Hidden
    @GetMapping("/read")
    public ResponseEntity<DataResponse<OrgResponse.Read>> getOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId
    ) {
        OrgResponse.Read response = orgService.getOrganization(userId);
        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }


    @PatchMapping("/{orgId}")
    public ResponseEntity<DataResponse<OrgResponse.Update>> modifyOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid OrgRequest.Update request
    )
    {
        OrgResponse.Update response = orgService.modifyOrganization(userId, orgId, request);
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

    @PostMapping("/members/{orgId}/invitation")
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> sendOrgInvitation(@PathVariable Long orgId, @RequestBody @Valid OrgRequest.Invite request) {
        OrgResponse.OrgInvitationResponse orgInvitationResponse = orgService.sendOrgInvitation(orgId, request.email());
        return ResponseEntity.ok(DataResponse.from(orgInvitationResponse));
    }

    @GetMapping("invitations/{token}")
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> acceptOrgInvitation(@PathVariable String token) {
        OrgResponse.OrgInvitationResponse orgInvitationResponse = orgService.acceptOrgInvitation(token);
        return ResponseEntity.ok(DataResponse.from(orgInvitationResponse));
    }
}
