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

    @Operation(
            summary = "알림 기록 조회 API (무한 스크롤 - Slice 기반)",
            description = "조직 내에서 본인이 수신한 알림 기록을 조회합니다. 안 읽은 알림이 먼저 오고, 안 읽은 알림끼리는 최신순입니다. \n\n" +
                    "안 읽은 알림이 없으면 전체가 최신순으로 정렬됩니다. cursor와 size 파라미터를 통해 무한 스크롤을 지원하며, cursor는 Base64로 인코딩된 문자열입니다. \n\n" +
                    "**읽음 처리 후에는 정렬 순서가 바뀌므로 cursor를 버리고 첫 페이지부터 다시 조회해야 합니다.**"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (hasNext: 다음 페이지 존재 여부, nextCursor: 다음 페이지 커서, notifications: 알림 기록 리스트)"),
            @ApiResponse(responseCode = "400", description = "CURSOR_400_3 : 잘못된 커서 형식\n\n NOTIFICATION_400_5 : 유효하지 않은 커서"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<NotificationResponse.NotificationHistoryList>> getHistory(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    );

    @Operation(summary = "알림 단건 읽음 처리",
            description = "알림 기록 하나를 읽음 상태로 변경합니다. userNotificationId 는 알림 기록 조회 응답의 userNotificationId 값을 그대로 사용합니다. " +
                    "이미 읽은 알림이면 아무 변화 없이 성공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_404_1 : 해당 조직의 멤버가 아닙니다.\n\n NOTIFICATION_404_3 : 알림 기록을 찾을 수 없습니다.")
    })
    ResponseEntity<DataResponse<Void>> markNotificationAsRead(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long userNotificationId
    );

    @Operation(summary = "알림 모두 읽음 처리",
            description = "조직 내에서 본인이 수신한 안 읽은 알림을 모두 읽음 상태로 변경합니다. 안 읽은 알림이 없으면 아무 변화 없이 성공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_404_1 : 해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> markAllNotificationsAsRead(
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

    @Operation(summary = "알림 채널 설정 변경", description = "회원 개인의 브라우저 푸시, 이메일 알림 채널을 설정합니다.")
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

    @Operation(summary = "알림 목표 설정 변경", description = "비즈니스 알림 트리거(클릭 관련, 주·일간 보고서)를 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateAlerts(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateAlerts request
    );

    @Operation(summary = "조직 알림 설정 변경",
            description = "조직 공용 외부 채널(Slack/Discord) 웹훅 연결·활성화와, 외부 채널로 내보낼 알림 종류(클릭 급증, 봇 클릭 감지, 주·일간 보고서)를 설정합니다. ADMIN만 접근 가능합니다. " +
                    "웹훅 URL은 값이 있으면 설정, disconnectSlack/disconnectDiscord=true면 삭제(연결 해제), 둘 다 없으면 변경 없음입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "NOTIFICATION_400_2: URL 없이 알림 활성화\n\n NOTIFICATION_400_3 : 활성화+연결해제 동시 요청\n\n NOTIFICATION_400_4 : 웹훅 URL 형식이 올바르지 않습니다."),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> updateOrgSettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateOrgSettings request
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

    @Operation(
            summary = "[테스트] 주간 리포트 이메일 발송",
            description = "지정된 조직의 주간 광고 리포트를 즉시 생성하여 이메일로 발송합니다. " +
                    "alertReport=true, isEmailEnabled=true, isMasterEnabled=true인 멤버에게만 발송됩니다. " +
                    "최근 7일치 MetricFact 데이터가 없으면 발송되지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 시작 성공"),
            @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없습니다.")
    })
    ResponseEntity<DataResponse<String>> testWeeklyReport(@PathVariable Long orgId);

    // ---------------- Web Push (VAPID) ----------------

    @Operation(
            summary = "VAPID 공개키 조회",
            description = "프론트에서 pushManager.subscribe({ applicationServerKey }) 호출 시 사용할 VAPID 공개키(Base64URL) 를 반환합니다. " +
                    "값은 서버에서 환경변수로 고정 관리되며 변하지 않습니다."
    )
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    ResponseEntity<DataResponse<NotificationResponse.VapidPublicKey>> getVapidPublicKey();

    @Operation(
            summary = "브라우저 푸시 구독 등록",
            description = "프론트가 서비스 워커에서 pushManager.subscribe() 결과로 얻은 subscription 을 서버에 저장합니다. " +
                    "같은 멤버십에 endpoint 가 이미 등록돼 있으면 해당 구독 정보를 갱신합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 등록 성공"),
            @ApiResponse(responseCode = "400", description = "NOTIFICATION_400_6 : 구독 정보가 올바르지 않습니다."),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_404_1 : 해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> subscribePush(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.PushSubscribe request
    );

    @Operation(
            summary = "브라우저 푸시 구독 해제",
            description = "endpoint 를 body 로 받아 해당 구독을 삭제합니다. 브라우저에서 pushManager.unsubscribe() 호출 후 서버 상태를 정리할 때 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구독 해제 성공"),
            @ApiResponse(responseCode = "400", description = "COMMON_400_2 : endpoint 는 필수입니다."),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_404_1 : 해당 조직의 멤버가 아닙니다.")
    })
    ResponseEntity<DataResponse<Void>> unsubscribePush(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.PushUnsubscribe request
    );
}
