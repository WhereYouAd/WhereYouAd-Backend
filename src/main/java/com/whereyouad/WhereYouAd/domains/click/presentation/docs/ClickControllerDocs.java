package com.whereyouad.WhereYouAd.domains.click.presentation.docs;

import com.whereyouad.WhereYouAd.domains.click.application.dto.request.ClickRequest;
import com.whereyouad.WhereYouAd.domains.click.application.dto.response.ClickResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Click API", description = "광고 클릭 추적, 더미 클릭 발생 API")
public interface ClickControllerDocs {

    @Operation(
            summary = "트래킹 링크 발급 API",
            description = "광고(adContentId)에 대한 트래킹 URL 발급\n\n" +
                    "- 하나의 광고당 URL은 1개만 존재\n" +
                    "- 이미 발급된 URL이 있으면 기존 URL을 그대로 반환\n" +
                    "- 트래킹 링크를 발급하기 위해서 landingURL이 있어야 정상적으로 redirect 되므로 필수적으로 입력"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "성공 (trackingUrl 반환)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요 (토큰 없음 또는 만료)"),
            @ApiResponse(responseCode = "403", description = "해당 조직의 구성원이 아닌 경우"),
            @ApiResponse(responseCode = "404", description = "해당 adContentId의 광고가 존재하지 않는 경우")
    })
    ResponseEntity<DataResponse<ClickResponse.NewTrackingUrl>> createTrackingUrl(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long adContentId,
            @Valid @RequestBody ClickRequest.CreateTrackingUrl request
    );

    @Operation(
            summary = "광고 트래킹 리다이렉트 API",
            description = "발급된 트래킹 링크 접속 시 클릭 이벤트 기록 후 광고 페이지로 리다이렉트"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "리다이렉트 성공"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 트래킹 링크")
    })
    ResponseEntity<Void> processTracking(
            @PathVariable String code,
            HttpServletRequest request
    );

    @Operation(
            summary = "(임시) 실시간 클릭수 데이터 조회 API",
            description = "실시간 클릭수 데이터를 dummy 또는 실제 집계값으로 확인\n" +
                    "- mode는 dummy 혹은 real, minutes에 원하는 집계 구간 (120분까지 가능) 입력"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "해당 adContentId의 광고가 존재하지 않는 경우")
    })
    ResponseEntity<DataResponse<java.util.List<ClickResponse.RealtimeClickCount>>> getRealtimeClickCounts(
            @PathVariable Long adContentId,
            @RequestParam(defaultValue = "real") String mode,
            @RequestParam(defaultValue = "60") int minutes
    );

    @Operation(
            summary = "실시간 클릭수 dummy 데이터 발생 여부 조정용 토글 API",
            description = "해당 요청을 통해 isRunning = true가 된다면, 실시간 클릭수를 더미 데이터로 500ms마다 발생시켜 1분 단위로 집계 되게끔 할 수 있음. (결과는 실시간 데이터 조회 API 응답에서 확인)\n" +
                    "- 사용을 끝낸 이후, 미사용 시 isRunning = false가 되게끔 토글시켜야 함. (불필요한 더미 데이터 발생 방지)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공")
    })
    ResponseEntity<DataResponse<String>> toggleDummyProducer(
    );
}
