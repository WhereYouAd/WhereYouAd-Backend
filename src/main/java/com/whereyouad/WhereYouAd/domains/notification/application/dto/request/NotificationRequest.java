package com.whereyouad.WhereYouAd.domains.notification.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class NotificationRequest {

    // 전체 알림 설정 DTO
    public record UpdateMaster(
            Boolean isMasterEnabled
    ) {}

    // 변경: 멤버 스코프(브라우저/이메일)만 남김. 슬랙/디스코드 웹훅·활성화는 UpdateOrgSettings 로 이동
    public record UpdateChannels(
            Boolean isBrowserPushEnabled,
            Boolean isEmailEnabled
    ) {}

    // 알림 기준 설정 DTO -> 각 멤버
    public record UpdateAlerts(
            Boolean alertClicks,
            Boolean alertReport
    ) {}

    // 조직 단위 외부 채널 알림 설정 (ADMIN 전용)
    // 외부 채널 웹훅 연결/활성화 + 외부 채널로 내보낼 알림 종류 설정을 하나의 DTO 로 통합
    public record UpdateOrgSettings(
            Boolean isSlackEnabled,
            @Pattern(
                    regexp = "^\\s*$|^https://hooks\\.slack\\.com/services/.+",
                    message = "슬랙 웹훅 URL 형식이 올바르지 않습니다."
            )
            String slackWebhookUrl,
            Boolean disconnectSlack,
            Boolean isDiscordEnabled,
            @Pattern(
                    regexp = "^\\s*$|^https://(discord|discordapp)\\.com/api/webhooks/.+",
                    message = "디스코드 웹훅 URL 형식이 올바르지 않습니다."
            )
            String discordWebhookUrl,
            Boolean disconnectDiscord,
            Boolean alertClicks,
            Boolean alertReport
    ) {}

    // 알림을 받을 멤버 설정 DTO(ADMIN 전용)
    public record UpdateMemberReceive(
            @NotNull(message = "membershipId는 필수입니다.")
            Long membershipId,
            boolean isReceive
    ) {}

    // 멤버 알림 설정 DTO
    public record BulkUpdateMembers(
            @Valid
            @NotEmpty(message = "members는 비어있을 수 없습니다.")
            List<UpdateMemberReceive> members
    ) {}

    public record TestSend(
            @NotBlank(message = "제목은 필수입니다.")
            String title,
            @NotBlank(message = "메시지는 필수입니다.")
            String message
    ) {}

    // 브라우저 pushManager.subscribe() 결과를 그대로 담는 구조
    public record PushSubscribe(
            @NotBlank(message = "endpoint 는 필수입니다.")
            String endpoint,

            @NotNull(message = "keys 는 필수입니다.")
            @Valid
            Keys keys,

            // 브라우저가 반환하는 만료 시각(ms epoch). 대개 null.
            Long expirationTime,

            String userAgent
    ) {
        @AssertTrue(message = "endpoint 는 유효한 HTTPS URI여야 합니다.")
        public boolean isEndpointValid() {
            if (endpoint == null || endpoint.isBlank()) {
                return false;
            }
            try {
                URI uri = new URI(endpoint);
                return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
            } catch (URISyntaxException e) {
                return false;
            }
        }

        public boolean isValidPushSubscription() {
            return isEndpointValid() && keys != null && keys.isP256dhValid() && keys.isAuthValid();
        }

        public record Keys(
                @NotBlank(message = "keys.p256dh 는 필수입니다.")
                String p256dh,

                @NotBlank(message = "keys.auth 는 필수입니다.")
                String auth
        ) {
            private static final java.util.regex.Pattern BASE64_URL_PATTERN =
                    java.util.regex.Pattern.compile("^[A-Za-z0-9_-]+={0,2}$");

            @AssertTrue(message = "keys.p256dh 는 65바이트 Base64URL 값이어야 합니다.")
            public boolean isP256dhValid() {
                return hasDecodedLength(p256dh, 65);
            }

            @AssertTrue(message = "keys.auth 는 16바이트 Base64URL 값이어야 합니다.")
            public boolean isAuthValid() {
                return hasDecodedLength(auth, 16);
            }

            private static boolean hasDecodedLength(String value, int expectedLength) {
                if (value == null || value.isBlank() || !BASE64_URL_PATTERN.matcher(value).matches()) {
                    return false;
                }
                try {
                    return Base64.getUrlDecoder().decode(value).length == expectedLength;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }
    }

    public record PushUnsubscribe(
            @NotBlank(message = "endpoint 는 필수입니다.")
            String endpoint
    ) {}
}
