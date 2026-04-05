package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaRequest;
import com.whereyouad.WhereYouAd.infrastructure.client.meta.dto.MetaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface MetaAdApiControllerDocs {

    @Operation(
            summary = "메타 OAuth 인증 URL 발급",
            description = "메타 광고 계정 연동을 시작하기 위한 페이스북 로그인(OAuth) 인증 URL을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 URL 발급 성공"),
            @ApiResponse(responseCode = "404", description = "ORG_404_1 : 존재하지 않는 조직 ID, ORG_404_2 : 해당 조직에 속하지 않은 회원의 요청")
    })
    ResponseEntity<DataResponse<MetaResponse.AuthUrlResponse>> getAuthUrl(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestParam Long orgId
    );

    @Operation(
            summary = "메타 OAuth 콜백 처리 및 데이터 최초 동기화",
            description = "페이스북 로그인 완료 후 호출되는 callback 엔드포인트입니다. 전달받은 인가 코드(Code)로 액세스 토큰을 발급받아 저장하고, 즉시 연동된 메타 광고 데이터를 최초 1회 동기화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 교환 및 데이터 동기화 성공 (동기화된 개수 반환)"),
            @ApiResponse(responseCode = "400", description = "인가 코드 만료/오류 또는 연동할 수 있는 메타 광고 계정이 없음"),
            @ApiResponse(responseCode = "500", description = "토큰 정보 암호화 저장 실패 또는 데이터 맵핑 파싱 오류")
    })
    ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> callback(
            @RequestParam String code,
            @Parameter(description = "OAuth state 파라미터 값", required = true)
            @RequestParam(name = "state") String state
    );

    @Operation(
            summary = "메타 광고 데이터 수동 동기화 트리거(관리자용)",
            description = "특정 조직에 연동되어 있는 메타 광고 계정의 전체 데이터(캠페인, 광고세트, 광고, 성과 지표)를 지정한 날짜 범위 내에서 수동으로 갱신(동기화)합니다.\n\n"
                    + "반드시 최초 메타 계정 연동(GET /api/meta/auth-url 을 통해 받은 링크로 로그인 진행) 이후 해당 API 를 호출해야 정상 동작합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "광고 데이터 수동 동기화 성공 (동기화된 개수 반환)"),
            @ApiResponse(responseCode = "404", description = "해당 조직에 연동된 메타 계정 정보가 존재하지 않음"),
            @ApiResponse(responseCode = "500", description = "외부 광고 플랫폼(메타 API) 통신 오류")
    })
    ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> syncManually(
            @PathVariable Long orgId,
            @RequestBody @Valid MetaRequest.MetaManualSyncRequest request
    );
}
