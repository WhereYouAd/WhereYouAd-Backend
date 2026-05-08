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
}
