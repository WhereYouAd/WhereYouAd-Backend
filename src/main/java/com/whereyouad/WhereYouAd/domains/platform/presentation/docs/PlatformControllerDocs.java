package com.whereyouad.WhereYouAd.domains.platform.presentation.docs;

import com.whereyouad.WhereYouAd.domains.platform.application.dto.request.PlatformRequest;
import com.whereyouad.WhereYouAd.domains.platform.application.dto.response.PlatformResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface PlatformControllerDocs {

    @Operation(
            summary = "네이버 광고 계정 등록 API",
            description = "네이버 광고 계정의 고객 ID, API 키(AES 암호화), Secret 키(AES 암호화)를 받아 " +
                    "실제 네이버 광고 API로 자격증명을 검증한 뒤 DB에 암호화 상태로 저장합니다. " +
                    "ADMIN 권한을 가진 조직 멤버만 호출할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400_1", description = "네이버 광고 API 인증 실패 (잘못된 자격증명)"),
            @ApiResponse(responseCode = "403_1", description = "ADMIN 권한 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직의 멤버가 아님")
    })
    ResponseEntity<DataResponse<PlatformResponse.PlatformAccount>> addNaverAdAccount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody PlatformRequest.PlatformAccount dto
    );

    @Operation(
            summary = "조직별 광고 플랫폼 연동 정보 조회 API",
            description = "로그인한 사용자가 특정 조직에 직접 연동한 광고 플랫폼(Naver/Meta/Google) 계정 목록을 조회합니다. \n\n"
                    +"ADMIN 권한을 가진 조직 멤버만 호출 가능하며, ADMIN이라도 본인이 연동한 계정만 조회됩니다 (본인 연동 계정이 없으면 빈 리스트 반환)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403_1", description = "ADMIN 권한 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직의 멤버가 아님")
    })
    ResponseEntity<DataResponse<PlatformResponse.PlatformAccountListResponse>> getPlatformSyncInfos(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );

    @Operation(
            summary = "네이버 광고 계정 자격증명 수정 API",
            description = "기존에 연동된 네이버 광고 계정의 API 키(AES 암호화) 및 Secret 키(AES 암호화)를 갱신합니다. " +
                    "요청 본문의 customerId(외부 고객 ID)로 기존 계정을 조회하며, 조회되지 않으면 404를 반환합니다. \n\n" +
                    "새 자격증명은 실제 네이버 광고 API 호출로 검증한 뒤 관련 키 값과 secret 값만 갱신합니다. " +
                    "ADMIN 권한을 가진 조직 멤버만 호출할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400_1", description = "네이버 광고 API 인증 실패"),
            @ApiResponse(responseCode = "403_1", description = "ADMIN 권한 없음"),
            @ApiResponse(responseCode = "404_1", description = "해당 계정에 대한 연동 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "404_2", description = "해당 조직의 회원이 아님"),
            @ApiResponse(responseCode = "404_3", description = "해당 customerId로 등록된 광고 계정을 찾을 수 없음")
    })
    ResponseEntity<DataResponse<PlatformResponse.PlatformAccount>> updateNaverAdAccount(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody PlatformRequest.PlatformAccount request
    );
}
