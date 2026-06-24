package com.whereyouad.WhereYouAd.domains.notification.presentation;

import com.whereyouad.WhereYouAd.domains.notification.application.dto.request.NotificationRequest;
import com.whereyouad.WhereYouAd.domains.notification.application.dto.response.NotificationResponse;
import com.whereyouad.WhereYouAd.domains.notification.domain.service.OrgNotificationSettingService;
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

    private final OrgNotificationSettingService orgNotificationSettingService;

    @PutMapping("/org/{orgId}/channels")
    public ResponseEntity<DataResponse<NotificationResponse.ChannelsListResponse>> upsertChannels(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid NotificationRequest.ChannelSettingRequest request
    ) {
        NotificationResponse.ChannelsListResponse response =
                orgNotificationSettingService.upsertChannels(userId, orgId, request);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @GetMapping("/org/{orgId}/channels")
    public ResponseEntity<DataResponse<NotificationResponse.ChannelsListResponse>> getChannels(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    ) {
        NotificationResponse.ChannelsListResponse response =
                orgNotificationSettingService.getChannels(userId, orgId);

        return ResponseEntity.ok(DataResponse.from(response));
    }

    @Hidden
    @PostMapping("/org/{orgId}/test")
    public ResponseEntity<DataResponse<String>> sendTest(
            @PathVariable Long orgId,
            @RequestBody @Valid NotificationRequest.TestSend request
    ) {
        orgNotificationSettingService.sendTest(orgId, request);

        return ResponseEntity.ok(DataResponse.from("테스트 알림을 발송했습니다."));
    }
}
