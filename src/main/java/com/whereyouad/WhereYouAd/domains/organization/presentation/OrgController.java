package com.whereyouad.WhereYouAd.domains.organization.presentation;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgCRUDService;
import com.whereyouad.WhereYouAd.domains.organization.presentation.docs.OrgControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/org")
public class OrgController implements OrgControllerDocs {

    private final OrgCRUDService orgService;

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
    public ResponseEntity<Void> modifyOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody OrgRequest.Update request
    )
    {
        orgService.modifyOrganization(userId, orgId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{orgId}")
    public ResponseEntity<Void> removeOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    )
    {
        orgService.removeOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }
}
