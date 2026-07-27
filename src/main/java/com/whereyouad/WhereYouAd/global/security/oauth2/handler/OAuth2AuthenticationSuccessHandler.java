package com.whereyouad.WhereYouAd.global.security.oauth2.handler;

import com.whereyouad.WhereYouAd.domains.user.persistence.entity.RefreshToken;
import com.whereyouad.WhereYouAd.domains.user.persistence.repository.RefreshTokenRepository;
import com.whereyouad.WhereYouAd.domains.user.domain.service.oauth.SocialOAuthTokenService;
import com.whereyouad.WhereYouAd.global.security.jwt.JwtTokenProvider;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import com.whereyouad.WhereYouAd.global.security.oauth2.dto.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    private final SocialOAuthTokenService socialOAuthTokenService;

    // TODO: 연동 시 프론트 주소로 변경(일단 스웨거로 redirect)
    @Value("${oauth2.redirect-url:http://localhost:8080/swagger-ui/index.html}")
    private String redirectUrl;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // CustomOAuth2User에서 사용자 정보 추출
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 성공 Handler에서는 AuthorizedClient를 통해 UserService 단계에 없던 RefreshToken까지 조회할 수 있다.
        OAuth2AuthenticationToken oAuth2Authentication = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                oAuth2Authentication.getAuthorizedClientRegistrationId(),
                oAuth2Authentication.getName()
        );
        // AuthorizedClient가 정상적으로 저장된 경우 연동 해제에 사용할 최신 OAuth 토큰을 보관한다.
        if (authorizedClient != null) {
            socialOAuthTokenService.saveTokens(
                    oAuth2User.getEmail(),
                    oAuth2User.getProvider(),
                    authorizedClient.getAccessToken(),
                    authorizedClient.getRefreshToken()
            );
        }

        // 토큰 제작을 위한 이메일 추출
        String email = oAuth2User.getEmail();

        // JWT 토큰 생성을 위한 Authentication 객체 (소셜 로그인 성공 시 발급됨)
        TokenResponse tokenResponse = jwtTokenProvider.generateToken(authentication);

        // RefreshToken DB에 저장
        RefreshToken refreshToken = refreshTokenRepository.findByKeyId(email)
                .map(entity -> entity.updateValue(tokenResponse.refreshToken()))
                .orElse(RefreshToken.builder()
                        .keyId(email)
                        .value(tokenResponse.refreshToken())
                        .build());

        refreshTokenRepository.save(refreshToken);

        // Access Token 쿠키 설정(httpOnly: false)
        ResponseCookie accessTokenCookie = baseCookie("access_token", tokenResponse.accessToken())
                .httpOnly(false)
                .maxAge(60 * 60) // 1시간
                .build();

        // Refresh Token을 HttpOnly 쿠키로 설정
        ResponseCookie refreshTokenCookie = baseCookie("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 7) // 7일
                .build();

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // 리다이렉트 (토큰은 쿠키로 전달)
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // 환경 설정(cookie.secure / cookie.domain / cookie.same-site)을 반영한 쿠키 빌더
    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite);
        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        return builder;
    }
}
