package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.domains.user.domain.service.AuthService;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.AuthControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<DataResponse<TokenResponse>> login(@RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        ResponseCookie httpOnlyCookie = baseCookie("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 7) // 7일
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, httpOnlyCookie.toString()) //생성한 RefreshToken 쿠키를 헤더에 설정
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken()) //AccessToken 을 Authorization 헤더에도 추가(편의사항)
                .body(DataResponse.from(tokenResponse));
    }

    @PostMapping("/reissue")
    public ResponseEntity<DataResponse<TokenResponse>> reIssue(
            @CookieValue(name = "refresh_token") String refreshToken
    )
    {
        TokenResponse tokenResponse = authService.reIssue(refreshToken);

        ResponseCookie httpOnlyCookie = baseCookie("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
                .maxAge(60 * 60 * 24 * 7) // 7일
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, httpOnlyCookie.toString()) //생성한 RefreshToken 쿠키를 헤더에 설정
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken()) //AccessToken 을 Authorization 헤더에도 추가(편의사항)
                .body(DataResponse.from(tokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(name = "Authorization") String authorizationHeader
    )
    {
        // Authorization 헤더에서 토큰 추출
        // SecurityConfig 에서 인증 필수이므로 헤더 존재 보장
        String accessToken = null;
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }

        authService.logout(accessToken);

        //RefreshToken 쿠키 제거
        ResponseCookie expiredRefresh = baseCookie("refresh_token", "")
                .httpOnly(true)
                .maxAge(0)
                .build();

        //AccessToken 쿠키 제거 — 소셜 로그인에서 OAuth2 핸들러가 세팅한 쿠키 제거용(이메일 로그인 유저 브라우저엔 해당 쿠키가 없어 해당X)
        ResponseCookie expiredAccess = baseCookie("access_token", "")
                .httpOnly(false)
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .build();
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
