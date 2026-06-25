package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.domains.advertisement.application.dto.request.AdvertisementRequest;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
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
            description = "페이스북 로그인 완료 후 사용자 브라우저가 호출하는 callback 엔드포인트입니다. "
                    + "전달받은 인가 코드(Code)로 액세스 토큰을 발급받아 저장하고, 즉시 연동된 메타 광고 데이터를 최초 1회 동기화합니다.\n\n"
                    + "처리 결과는 JSON 응답이 아닌 프론트엔드 결과 페이지로의 302 리다이렉트로 반환됩니다.(추후 프론트 처리 경로 받아서 수정 필요. 현재는 http://localhost:5173/oauth2/meta/result 로 되어있음.)\n\n"
                    + "처리 결과에 따라 리다이렉트 URL의 쿼리 파라미터가 다릅니다.\n\n"
                    + "- 성공 시: status(success / partial)와 함께 동기화된 개수가 전달됩니다. "
                    + "(adCampaigns: 캠페인, adGroups: 광고세트, adContents: 광고, metricFacts: 성과 지표, "
                    + "일부 계정 동기화 실패 시 status=partial과 함께 failedCount: 실패한 계정 수가 추가로 전달)\n\n"
                    + "- 오류 시: status(denied / invalid_request / error)와 "
                    + "필요 시 detail이 함께 전달됩니다.\n\n"
                    + "  · status=denied → 사용자가 Meta 동의 화면에서 거부한 경우. detail에는 Meta가 전달한 표준 에러값(예: access_denied)이 그대로 담깁니다.\n\n"
                    + "  · status=invalid_request → 필수 파라미터(code, state) 누락. detail 없음.\n\n"
                    + "  · status=error → 콜백 처리 도중 서버 측 실패. detail은 다음 중 하나만 노출됩니다:\n\n"
                    + "    - no_ad_account : 연동 가능한 Meta 광고 계정이 없는 경우. 사용자가 Meta 광고 관리자에서 광고 계정을 먼저 생성해야 함을 안내해야 합니다.\n\n"
                    + "    - meta_oauth_failed : 그 외 모든 실패(state 파싱 실패, 토큰 교환 실패, 광고 계정 조회 실패, DB 저장 실패 등). 내부 에러 코드는 외부에 노출하지 않으며 사용자에게는 오류발생, 재시도 권장 으로 표시합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "처리 결과를 프론트엔드 결과 페이지로 리다이렉트")
    })
    ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    );

    @Operation(
            summary = "메타 광고 데이터 수동 동기화 트리거(디버깅, 테스팅 용도)",
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
            @RequestBody @Valid AdvertisementRequest.ManualSyncRequest request
    );

    @Operation(
            summary = "Meta 광고 데이터 갱신(사용자 새로고침 요청 처리용)",
            description = "사용자가 Meta 마케팅 정보에 대해 '갱신(refresh)' 버튼 클릭 시 처리하는 API 입니다. 마케팅 정보 갱신은 관리자(role:ADMIN) 만 가능합니다."
                    + "관리자용 동기화(/sync)와 달리, 사용자에 대한 조직 멤버 검증과, 같은 조직에 대한 반복 요청을 60초 간격으로 제한하고, 동기화 기간은 최근 7일로 고정되어있습니다.\n\n"
                    + "반드시 최초 Meta 계정 연동이 되어있어야 정상 동작합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (동기화된 개수 반환)"),
            @ApiResponse(responseCode = "404", description = "ORG_404_1 : 존재하지 않는 조직 ID, ORG_404_2 : 해당 조직에 속하지 않은 회원의 요청"),
            @ApiResponse(responseCode = "409", description = "ADAPI_409_1 : 해당 조직의 광고 데이터 동기화가 이미 진행 중"),
            @ApiResponse(responseCode = "429", description = "ADAPI_429_1 : 너무 잦은 동기화 요청(쿨다운)"),
            @ApiResponse(responseCode = "500", description = "외부 광고 플랫폼(메타 API) 통신 오류")
    })
    ResponseEntity<DataResponse<MetaResponse.MetaSyncSummary>> refreshForUser(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );

    @Operation(
            summary = "메타 캠페인 예산 변경",
            description = "메타 캠페인의 예산을 변경합니다. 일일 예산(dailyBudget)과 총 예산(lifetimeBudget) 중 정확히 하나만 입력해야 하며, "
                    + "캠페인에 기존에 설정된 예산 유형과 동일한 유형으로만 변경할 수 있습니다.\n\n"
                    + "해당 광고 계정을 연동한 사용자(소유자)만 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예산 변경 성공"),
            @ApiResponse(responseCode = "400", description =
                    "COMMON_400_2 : 요청 본문 검증 실패(일일/총 예산 중 정확히 하나만 입력)\n\n "
                    + "ADAPI_400_2 : 메타 캠페인이 아닌 대상에 대한 요청\n\n"
                    + "META_400_1 : 예산이 Meta 최소 기준 미만\n\n"
                    + "META_400_2 : 기존과 다른 예산 유형으로 변경 요청\n\n"
                    + "META_400_3 : 기존 예산 값과 같은 값으로 변경 불가"),
            @ApiResponse(responseCode = "401", description = "ADAPI_401_1 : 연동 인증 정보(토큰)가 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "403", description = "ADAPI_403_2 : 해당 광고 계정을 연동한 사용자가 아닌 회원의 요청"),
            @ApiResponse(responseCode = "404", description = "AD_404_1 : 존재하지 않는 캠페인 ID"),
            @ApiResponse(responseCode = "502", description = "ADAPI_502_3 : Meta 예산 변경 요청 실패")
    })
    ResponseEntity<DataResponse<MetaResponse.BudgetUpdateResponse>> updateCampaignBudget(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long adCampaignId,
            @RequestBody @Valid AdvertisementRequest.MetaBudgetUpdateRequest request
    );

    @Operation(
            summary = "메타 광고그룹 예산 변경",
            description = "메타 광고그룹(AdSet)의 예산을 변경합니다. 일일 예산(dailyBudget)과 총 예산(lifetimeBudget) 중 정확히 하나만 입력해야 하며, "
                    + "광고 그룹에 기존에 설정된 예산 유형과 동일한 유형으로만 변경할 수 있습니다.\n\n"
                    + "해당 광고 계정을 연동한 사용자(소유자)만 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "예산 변경 성공"),
            @ApiResponse(responseCode = "400", description =
                    "COMMON_400_2 : 요청 본문 검증 실패(일일/총 예산 중 정확히 하나만 입력)\n\n"
                            + "ADAPI_400_2 : 메타 광고그룹이 아닌 대상에 대한 요청"
                            + "META_400_1 : 예산이 Meta 최소 기준 미만"
                            + "META_400_2 : 기존과 다른 예산 유형으로 변경 요청"
                            + "META_400_3 : 기존 예산 값과 같은 값으로 변경 불가"),
            @ApiResponse(responseCode = "401", description = "ADAPI_401_1 : 연동 인증 정보(토큰)가 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "403", description = "ADAPI_403_1 : 해당 광고 계정을 연동한 사용자가 아닌 회원의 요청"),
            @ApiResponse(responseCode = "404", description = "AD_404_2 : 존재하지 않는 광고그룹 ID"),
            @ApiResponse(responseCode = "502", description = "ADAPI_502_3 : 예산 변경 요청 실패")
    })
    ResponseEntity<DataResponse<MetaResponse.BudgetUpdateResponse>> updateAdGroupBudget(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long adGroupId,
            @RequestBody @Valid AdvertisementRequest.MetaBudgetUpdateRequest request
    );
}
