package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

public class SmsResponse {

        public record SmsSentResponse(
                        String message,
                        String phoneNumber, // 전송한 휴대폰 번호
                        long expireIn) {
        }

        public record SmsVerifiedResponse(
                        boolean isVerified,
                        String verificationMessage) {
        }
}
