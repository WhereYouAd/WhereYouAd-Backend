package com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs;

import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

public interface GoogleAdOAuthDocs {

    @Operation(
            summary = "구글 광고 연동을 위한 OAuth 로그인 리다이렉트",
            description = "구글 Ads 플랫폼 연동을 위해 사용자를 구글 로그인 및 권한 동의 페이지로 리다이렉트 시킵니다. " +
                    "인증이 완료되면 사전에 등록된 콜백(callback) URI로 인증 코드(code)와 상태 값(state)이 반환됩니다."
    )
    @GetMapping("/login")
    void redirectToGoogleAuth(@Parameter @RequestParam("orgId") Long orgId,
                              @Parameter(hidden = true) @AuthenticationPrincipal(expression = "userId") Long userId,
                              HttpServletResponse response) throws IOException;

    @Operation(
            summary = "구글 OAuth 인증 콜백 및 리프레시 토큰 발급",
            description = "구글 로그인 후 반환된 인증 코드(code)를 이용해 구글 서버와 통신하여 리프레시 토큰(Refresh Token)을 발급받습니다. " +
                    "이후 연동된 구글 광고 계정 목록을 조회하여 DB에 플랫폼 계정과 연동 정보를 저장합니다."
    )
    @GetMapping("/callback")
    void exchangeCodeForToken(
            @Parameter(description = "구글 인증 서버로부터 반환된 일회성 인증 코드") @RequestParam("code") String code,
            @Parameter(description = "요청 시 전달했던 조직 정보가 인코딩된 상태 값") @RequestParam("state") String state) throws IOException;
}
