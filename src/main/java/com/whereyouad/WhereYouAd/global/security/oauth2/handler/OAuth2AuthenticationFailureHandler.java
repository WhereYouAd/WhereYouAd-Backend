package com.whereyouad.WhereYouAd.global.security.oauth2.handler;

import com.whereyouad.WhereYouAd.global.security.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    // 실패 시 리다이렉트할 프론트엔드 주소 (환경변수 OAUTH2_REDIRECT_URL로 설정)
    @Value("${oauth2.redirect-url:http://localhost:3000/oauth2/redirect}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // 로그인 시작 시 저장했던 state 쿠키 삭제
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequest(request, response);
        // 프론트엔드로 에러 메시지와 함께 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUrl + "?error=" + exception.getMessage());
    }
}
