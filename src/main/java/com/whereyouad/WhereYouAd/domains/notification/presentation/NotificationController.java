package com.whereyouad.WhereYouAd.domains.notification.presentation;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.NotificationService;
import com.whereyouad.WhereYouAd.domains.notification.presentation.docs.NotificationControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final NotificationService notificationService;

    @GetMapping("/settings/{orgId}")
    @Override
    public ResponseEntity<DataResponse<NotificationResponse.MySettings>> getMySettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    ) {
        return ResponseEntity.ok(DataResponse.from(notificationService.getMySettings(userId, orgId)));
    }

    @PatchMapping("/settings/{orgId}/master")
    @Override
    public ResponseEntity<DataResponse<Void>> updateMaster(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateMaster request
    ) {
        notificationService.updateMaster(userId, orgId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @PatchMapping("/settings/{orgId}/channels")
    @Override
    public ResponseEntity<DataResponse<Void>> updateChannels(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateChannels request
    ) {
        notificationService.updateChannels(userId, orgId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @PatchMapping("/settings/{orgId}/alerts")
    @Override
    public ResponseEntity<DataResponse<Void>> updateAlerts(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody NotificationRequest.UpdateAlerts request
    ) {
        notificationService.updateAlerts(userId, orgId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @GetMapping("/settings/{orgId}/members")
    @Override
    public ResponseEntity<DataResponse<NotificationResponse.MemberSettingList>> getMemberSettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(DataResponse.from(notificationService.getMemberSettings(userId, orgId, cursor, size)));
    }

    @PatchMapping("/settings/{orgId}/members")
    @Override
    public ResponseEntity<DataResponse<Void>> updateMemberSettings(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid NotificationRequest.BulkUpdateMembers request
    ) {
        notificationService.updateMemberSettings(userId, orgId, request);
        return ResponseEntity.ok(DataResponse.ok());
    }

    @Hidden
    @PostMapping("/org/{orgId}/test")
    public ResponseEntity<DataResponse<String>> sendTest(
            @PathVariable Long orgId,
            @RequestBody @Valid NotificationRequest.TestSend request
    ) {
        notificationService.sendTest(orgId, request);

        return ResponseEntity.ok(DataResponse.from("테스트 알림을 발송했습니다."));
    }
}
