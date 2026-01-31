package com.whereyouad.WhereYouAd.domains.user.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SmsRequest {
        public record SmsSendRequest(
                        @NotBlank(message = "전화번호는 필수입니다.")
                        @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 하이픈 없이 10~11자리 숫자만 입력해주세요.") String phoneNumber) {
        }

        public record SmsVerifyRequest(
                        @NotBlank(message = "전화번호는 필수입니다.") String phoneNumber,
                        @NotBlank(message = "인증코드는 필수입니다.") String verificationCode) {
        }
}
