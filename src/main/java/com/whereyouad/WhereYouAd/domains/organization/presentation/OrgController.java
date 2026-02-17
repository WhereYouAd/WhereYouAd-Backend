package com.whereyouad.WhereYouAd.domains.organization.presentation;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.domains.organization.domain.service.OrgService;
import com.whereyouad.WhereYouAd.domains.organization.presentation.docs.OrgControllerDocs;
import com.whereyouad.WhereYouAd.global.exception.AppException;
import com.whereyouad.WhereYouAd.global.exception.ErrorCode;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/my")
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getMyOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PageableDefault(size = 10) Pageable pageable
    )
    {
        OrgResponse.MyOrganizations response = orgService.getMyOrganizations(userId, pageable);

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

    @GetMapping("/search")
    public ResponseEntity<DataResponse<OrgResponse.OrgSearchList>> searchOrganizations(
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    )
    {
        //만약 조회하려는 조직 이름이 공백이라면(ex. /search?name=) 예외처리
        //DB 조회에서 like 로 처리하기 때문에 공백이 들어오면 DB 에 존재하는 모든 조직이 조회되는 문제가 있어 이를 방지
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }

        OrgResponse.OrgSearchList response = orgService.getOrganizationList(name, pageable);

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
}
