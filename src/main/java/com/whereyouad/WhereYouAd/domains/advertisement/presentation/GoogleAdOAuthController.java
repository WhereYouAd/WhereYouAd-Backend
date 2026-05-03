package com.whereyouad.WhereYouAd.domains.advertisement.presentation;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.service.adapi.google.GoogleAdOAuthService;
import com.whereyouad.WhereYouAd.domains.advertisement.presentation.docs.GoogleAdOAuthDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/google")
public class GoogleAdOAuthController implements GoogleAdOAuthDocs {

    @Value("${google.ads.client-id}")
    private String clientId;

    @Value("${google.ads.redirect-uri}")
    private String redirectUri;

    // 구글 광고 API 접근 권한 스코프
    private final String SCOPE = "https://www.googleapis.com/auth/adwords";

    private final GoogleAdOAuthService googleAdOAuthService;

    // 구글 연동 로그인 화면으로 리다이렉트
    @GetMapping("/login")
    public void redirectToGoogleAuth(@RequestParam("orgId") Long orgId, @AuthenticationPrincipal(expression = "userId") Long userId,
                                     HttpServletResponse response) throws IOException {

        String rawState = userId + "_" + orgId;
        String encodedState = Base64.getUrlEncoder().encodeToString(rawState.getBytes());

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + SCOPE +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + encodedState;

        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<DataResponse<String>> exchangeCodeForToken(@RequestParam("code") String code, @RequestParam("state") String state) throws IOException {
        // state 디코딩해서 userId, orgId 추출
        String decodedState = new String(Base64.getUrlDecoder().decode(state));
        String[] parts = decodedState.split("_");
        Long userId = Long.parseLong(parts[0]);
        Long orgId = Long.parseLong(parts[1]);

        String refreshToken = googleAdOAuthService.exchangeCodeAndSavePlatformConnection(userId, orgId, code);
        return ResponseEntity.ok(DataResponse.from(refreshToken));
    }
}
