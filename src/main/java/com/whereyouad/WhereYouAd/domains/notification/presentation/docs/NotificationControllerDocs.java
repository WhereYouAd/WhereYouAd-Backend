package com.whereyouad.WhereYouAd.domains.notification.presentation.docs;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification API", description = "알림 설정 API")
public interface NotificationControllerDocs {

    @Operation(summary = "내 알림 설정 조회", description = "현재 로그인한 사용자의 알림 설정을 조회합니다. 설정이 없을 경우 기본값으로 자동 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<NotificationResponse.MySettings>> getMySettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );

    @Operation(summary = "마스터 컨트롤 변경", description = "모든 알림을 일시적으로 켜거나 끕니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateMaster(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateMaster request
    );

    @Operation(summary = "알림 채널 설정 변경", description = "브라우저 푸시, 이메일, 슬랙, 디스코드 알림 채널을 설정합니다. " +
            "slackWebhookUrl/discordWebhookUrl은 ADMIN만 반영됩니다. " +
            "웹훅 URL은 값이 있으면 설정, disconnectSlack/disconnectDiscord=true면 삭제(연결 해제), 둘 다 없으면 변경 없음입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "NOTIFICATION_400_2: URL 없이 알림 활성화\n\n NOTIFICATION_400_3 : 활성화+연결해제 동시 요청"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateChannels(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateChannels request
    );

    @Operation(summary = "알림 목표 설정 변경", description = "비즈니스 알림 트리거(클릭 급증, 봇 클릭 감지, 주·일간 보고서)를 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateAlerts(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateAlerts request
    );

    @Operation(summary = "멤버 알림 설정 목록 조회", description = "조직 내 멤버별 알림 수신 여부를 조회합니다. ADMIN만 접근 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<NotificationResponse.MemberSettingList>> getMemberSettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    );

    @Operation(summary = "멤버 알림 수신 여부 변경", description = "조직 내 멤버들의 알림 수신 여부를 일괄 변경합니다. ADMIN만 접근 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateMemberSettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.BulkUpdateMembers request
    );
}
