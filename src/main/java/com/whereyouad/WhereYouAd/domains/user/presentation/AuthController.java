package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.domains.user.domain.service.AuthService;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.AuthControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<DataResponse<TokenResponse>> login(@RequestBody LoginRequest request) {

        TokenResponse tokenResponse = authService.login(request);

        ResponseCookie httpOnlyCookie = ResponseCookie.from("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
//                .secure(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24 * 7)
//                .sameSite("None")
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, httpOnlyCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken())
                .body(DataResponse.from(tokenResponse));
    }

    @PostMapping("/reissue")
    public ResponseEntity<DataResponse<TokenResponse>> reissue(
            @CookieValue(name = "refresh_token") String refreshToken
    )
    {
        TokenResponse tokenResponse = authService.reissue(refreshToken);

        ResponseCookie httpOnlyCookie = ResponseCookie.from("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
//                .secure(true)
                .secure(false) // HTTPS 적용 시 true로 변경
                .path("/")
                .maxAge(60 * 60 * 24 * 7) // 7일
//                .sameSite("None")
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, httpOnlyCookie.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken())
                .body(DataResponse.from(tokenResponse));
    }
}
