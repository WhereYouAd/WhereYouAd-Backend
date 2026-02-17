package com.whereyouad.WhereYouAd.domains.organization.presentation;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgQueryService;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.presentation.docs.OrgControllerDocs;
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
    private final OrgQueryService orgQueryService;

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

    @Hidden
    @DeleteMapping("/{orgId}")
    public ResponseEntity<Void> removeOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        orgService.removeOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
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

}
