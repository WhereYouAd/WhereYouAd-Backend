package com.whereyouad.WhereYouAd.domains.user.application.dto.response;

import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;

import java.util.List;

public record EmailSentResponse(
        String message, //인증코드를 이메일로 전송했습니다.
        String email, //전송한 이메일
        Long expireIn, //만료시간 (500L -> 500초)
        boolean isProviderLinked,
        List<Provider> providerTypes
) {
}
