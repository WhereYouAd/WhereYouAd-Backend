package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.domains.user.domain.service.AuthService;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.AuthControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.cookie.AuthCookieFactory;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
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
    private final AuthCookieFactory authCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<DataResponse<TokenResponse>> login(@RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        ResponseCookie httpOnlyCookie = authCookieFactory.createRefreshTokenCookie(
                tokenResponse.refreshToken(), 60 * 60 * 24 * 7); // 7일

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

        ResponseCookie httpOnlyCookie = authCookieFactory.createRefreshTokenCookie(
                tokenResponse.refreshToken(), 60 * 60 * 24 * 7); // 7일

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
        ResponseCookie expiredRefresh = authCookieFactory.createRefreshTokenCookie("", 0);

        //AccessToken 쿠키 제거 — 소셜 로그인에서 OAuth2 핸들러가 세팅한 쿠키 제거용(이메일 로그인 유저 브라우저엔 해당 쿠키가 없어 해당X)
        ResponseCookie expiredAccess = authCookieFactory.createAccessTokenCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .build();
    }

}
