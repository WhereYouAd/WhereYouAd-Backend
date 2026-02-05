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
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(
            summary = "조직 정보 수정 API",
            description = "새로운 조직 이름, 설명, 로고 이미지 URL 을 받아 저장(해당 조직을 생성한 회원만 정보 변경 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(변경된 필드 값들과 조직Id, 변경 시각 반환)"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X")
    })
    public ResponseEntity<DataResponse<OrgResponse.Update>> modifyOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid OrgRequest.Update request
    );
}
