package com.whereyouad.WhereYouAd.domains.organization.presentation.docs;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            summary = "내가 속한 조직 전체 조회 API",
            description = "로그인한 회원이 속한 조직들의 DB id, 이름, 설명, 로고URL, 내 역할(ADMIN/MEMBER) 을 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401_3", description = "토큰 없이 접근 시 실패")
    })
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getMyOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId);

    @Operation(
            summary = "조직 하나의 세부정보 조회 API",
            description = "조직 id 를 param 으로 받아 해당 조직의 id, 이름, 설명, 로고URL, 조직 생성 시각 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 값 조직 존재 X")
    })
    public ResponseEntity<DataResponse<OrgResponse.OrgDetail>> getOrganizationDetail(@PathVariable Long orgId);


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

    @Operation(
            summary = "조직 복구 API",
            description = "Soft Delete 로 임시삭제한 조직을 다시 활성화 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X"),
            @ApiResponse(responseCode = "409_1", description = "이미 활성화 상태인 조직")
    })
    public ResponseEntity<DataResponse<OrgResponse.Delete>> restoreOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );


    @Operation(
            summary = "조직 삭제 API",
            description = "조직 Id 를 PathVariable 로 받아 해당 조직 삭제(해당 조직을 생성한 회원만 삭제 가능) \n\n" +
                    "param 인 isHard = true 이면 Hard Delete (DB에서 삭제), isHard = false 이면 Soft Delete (status 만 DELETED 로 변경)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X")
    })
    public ResponseEntity<DataResponse<String>> removeOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "false") boolean isHard
    );
}
