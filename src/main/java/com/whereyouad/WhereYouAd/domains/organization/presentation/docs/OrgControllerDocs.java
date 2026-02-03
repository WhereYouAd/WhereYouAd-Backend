package com.whereyouad.WhereYouAd.domains.organization.presentation.docs;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrgControllerDocs {
    @Operation(
            summary = "조직 생성 API",
            description = "조직 이름, 설명, 로고 이미지 URL 을 받아 저장(로그인이 진행된 회원만 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_1", description = "조직 이름 중복")
    })
    public ResponseEntity<DataResponse<OrgResponse.Create>> createOrganization(@AuthenticationPrincipal(expression = "userId") Long userId,
                                                                               @RequestBody @Valid OrgRequest.Create request);
}
