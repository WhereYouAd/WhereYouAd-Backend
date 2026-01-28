package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

public record EmailSentResponse(
        String message, //인증코드를 이메일로 전송했습니다.
        String email, //전송한 이메일
        long expireIn //만료시간 (500L -> 500초)
) {
}
