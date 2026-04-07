package com.whereyouad.WhereYouAd.domains.platform.presentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GoogleAdOAuthController {

    @RestController
    @RequestMapping("/api/google")
    public class GoogleOAuthController {

        @Value("${google.ads.client-id}")
        private String clientId;

        @Value("${google.ads.client-secret}")
        private String clientSecret;

        @Value("${google.ads.redirect-uri}")
        private String redirectUri;

        // 구글 광고 API 접근 권한 스코프
        private final String SCOPE = "https://www.googleapis.com/auth/adwords";

        // 구글 연동 로그인 화면으로 리다이렉트
        @GetMapping("/login")
        public void redirectToGoogleAuth(HttpServletResponse response) throws IOException {
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + clientId +
                    "&redirect_uri=" + redirectUri +
                    "&response_type=code" +
                    "&scope=" + SCOPE +
                    "&access_type=offline" +
                    "&prompt=consent";

            response.sendRedirect(authUrl);
        }
    }
}
